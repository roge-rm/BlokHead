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
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.datastore.preferences)
        }
    }
}
