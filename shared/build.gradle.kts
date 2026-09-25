import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Everything BlokHead's Android app and its browser build have in common: the game itself, the
// Compose screens and the navigation between them. Platform pieces (rendering, storage, sound,
// input plumbing) sit behind small interfaces or expect/actual declarations, with the Android side
// in androidMain and the browser side in wasmJsMain. The Android app (:app) depends on this; the
// browser build is this module's wasmJs executable.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// The browser build shows the same version as the Android app, read from :app's build file.
val webBuildInfoDir = layout.buildDirectory.dir("generated/webBuildInfo")
val appVersionName = Regex("versionName = \"([^\"]+)\"")
    .find(rootProject.file("app/build.gradle.kts").readText())!!.groupValues[1]
val generateWebBuildInfo by tasks.registering {
    // Copied into locals so doLast captures values, not the build script (configuration cache).
    val outDir = webBuildInfoDir
    val version = appVersionName
    inputs.property("versionName", version)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/rm/blokhead/WebBuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("package com.rm.blokhead\n\ninternal const val VERSION_NAME = \"$version\"\n")
    }
}

kotlin {
    android {
        namespace = "com.rm.blokhead.shared"
        compileSdk = 37
        minSdk = 27
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "blokhead"
        browser {
            commonWebpackConfig {
                outputFileName = "blokhead.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jb.compose.runtime)
            implementation(libs.jb.compose.foundation)
            implementation(libs.jb.compose.ui)
            implementation(libs.jb.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
        wasmJsMain {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
            // The page's sound effects are the Android app's own WAVs, served beside index.html.
            resources.srcDir(rootProject.file("app/src/main/res/raw"))
            kotlin.srcDir(generateWebBuildInfo)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.datastore.preferences)
        }
    }
}

// Binaryen (wasm-opt) is set up per project; its download repository is declared in
// settings.gradle.kts, which refuses plugin-added ones.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
}
