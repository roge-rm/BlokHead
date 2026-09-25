package com.rm.blokhead

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rm.blokhead.audio.Sfx
import com.rm.blokhead.data.GamepadBindingsRepository
import com.rm.blokhead.data.HighScoreRepository
import com.rm.blokhead.data.SettingsRepository
import com.rm.blokhead.game.GameEngine

/** What [BlokHeadApp] needs from the platform it runs on. :app's AndroidPlatform backs it with
 *  DataStore, SoundPool and the GLES renderer; wasmJsMain's WebPlatform with localStorage, Web
 *  Audio and a Compose Canvas renderer. */
interface BlokHeadPlatform {
    val versionName: String

    /** Shown under the main menu's buttons; the browser build lists its keyboard controls here. */
    val menuFootnote: String? get() = null

    fun createSettingsRepository(): SettingsRepository
    fun createHighScoreRepository(): HighScoreRepository
    fun createGamepadBindingsRepository(): GamepadBindingsRepository
    fun createSfx(): Sfx

    /** One playthrough's view of [engine]. Created once per session (see GameScreen). */
    @Composable
    fun rememberGameSurface(sessionId: Int, engine: GameEngine): GameSurface

    /** The system Back button/gesture, where there is one. */
    @Composable
    fun BackHandler(enabled: Boolean, onBack: () -> Unit)

    /** Whether to use the landscape (pillarboxed) game layout rather than portrait's. */
    @Composable
    fun isLandscape(): Boolean
}

/** Renders a [GameEngine] and owns its game loop, advancing the engine each frame. Every engine
 *  mutation from the UI goes through [enqueue], so it runs on whichever thread owns the loop (the
 *  GL thread on Android). */
interface GameSurface {
    fun enqueue(action: GameEngine.() -> Unit)

    @Composable
    fun Content(modifier: Modifier)
}
