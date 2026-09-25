package com.rm.blokhead

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import com.rm.blokhead.audio.Sfx
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.data.GamepadBindings
import com.rm.blokhead.data.GamepadBindingsRepository
import com.rm.blokhead.data.HighScoreCodec
import com.rm.blokhead.data.HighScoreEntry
import com.rm.blokhead.data.HighScoreRepository
import com.rm.blokhead.data.HighScoreTable
import com.rm.blokhead.data.Settings
import com.rm.blokhead.data.SettingsRepository
import com.rm.blokhead.game.BlockSet
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.render.CanvasRenderer
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.MutableStateFlow
import org.w3c.dom.events.Event

/** [BlokHeadPlatform] for the browser: localStorage, Web Audio (see index.html's blokSfx) and
 *  [CanvasRenderer] driven by Compose's frame clock. */
object WebPlatform : BlokHeadPlatform {
    override val versionName: String = VERSION_NAME
    override val menuFootnote: String = KEYBOARD_HELP

    override fun createSettingsRepository(): SettingsRepository = LocalStorageSettings
    override fun createHighScoreRepository(): HighScoreRepository = LocalStorageHighScores
    override fun createGamepadBindingsRepository(): GamepadBindingsRepository = LocalStorageGamepadBindings
    override fun createSfx(): Sfx = WebSfx()

    @Composable
    override fun rememberGameSurface(sessionId: Int, engine: GameEngine): GameSurface =
        remember(sessionId) { WebGameSurface(engine) }

    /** Browsers have no Back button of their own for the app to use; Esc does the job instead
     *  (see [installKeyboardControls]). */
    @Composable
    override fun BackHandler(enabled: Boolean, onBack: () -> Unit) {}

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun isLandscape(): Boolean {
        val size = LocalWindowInfo.current.containerSize
        return size.width > size.height
    }
}

private class WebGameSurface(private val engine: GameEngine) : GameSurface {
    private val pending = ArrayDeque<GameEngine.() -> Unit>()
    private val renderer = CanvasRenderer()

    override fun enqueue(action: GameEngine.() -> Unit) {
        pending.addLast(action)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        var frameTime by remember { mutableLongStateOf(0L) }
        // The game loop: same order as BlokoutRenderer.onDrawFrame — queued input, then advance
        // by the real time since the last frame (clamped, so a stall never jumps the game).
        LaunchedEffect(this) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    while (pending.isNotEmpty()) engine.(pending.removeFirst())()
                    val delta = if (last == 0L) 0f else (now - last) / 1_000_000_000f
                    last = now
                    engine.update(delta.coerceAtMost(0.1f))
                    frameTime = now
                }
            }
        }
        // Switching tabs pauses the game, like backgrounding the Android app does.
        DisposableEffect(this) {
            val listener: (Event) -> Unit = {
                if (isPageHidden()) enqueue { setPaused(true) }
            }
            document.addEventListener("visibilitychange", listener)
            onDispose { document.removeEventListener("visibilitychange", listener) }
        }
        Canvas(modifier) {
            frameTime // redraw every frame
            with(renderer) { drawScene(engine) }
        }
    }
}

private class WebSfx : Sfx {
    override var muted: Boolean = false
    override fun playMove() = play("move")
    override fun playRotate() = play("rotate")
    override fun playLock() = play("lock")
    override fun playClear() = play("clear")
    override fun playGameOver() = play("gameover")
    override fun playMenu() = play("menu")
    override fun release() {}

    private fun play(name: String) {
        if (!muted) playSfx(name)
    }
}

private fun playSfx(name: String): Unit = js("window.blokSfx && window.blokSfx.play(name)")

private fun isPageHidden(): Boolean = js("document.visibilityState === 'hidden'")

private fun readStorage(key: String): String? = runCatching { localStorage.getItem(key) }.getOrNull()

private fun writeStorage(key: String, value: String) {
    runCatching { localStorage.setItem(key, value) }
}

/** "name=value" lines, one per [Settings] field; unknown or missing fields fall back to defaults. */
private object LocalStorageSettings : SettingsRepository {
    private const val KEY = "blokhead.settings"
    override val settings = MutableStateFlow(decode(readStorage(KEY)))

    override suspend fun save(settings: Settings) {
        writeStorage(KEY, encode(settings))
        this.settings.value = settings
    }

    private fun encode(s: Settings): String = listOf(
        "diagonalButtonsEnabled" to s.diagonalButtonsEnabled,
        "gestureControlsEnabled" to s.gestureControlsEnabled,
        "onScreenButtonsEnabled" to s.onScreenButtonsEnabled,
        "startingDifficulty" to s.startingDifficulty,
        "portraitButtonHeight" to s.portraitButtonHeight,
        "soundEnabled" to s.soundEnabled,
        "leftHandedMode" to s.leftHandedMode,
        "blockSet" to s.blockSet.name,
        "wellSize" to s.wellSize,
        "wellHeight" to s.wellHeight,
        "buttonOpacity" to s.buttonOpacity,
        "buttonScale" to s.buttonScale,
        "portraitButtonInset" to s.portraitButtonInset,
        "landscapeButtonInset" to s.landscapeButtonInset,
        "landscapeButtonHeight" to s.landscapeButtonHeight,
    ).joinToString("\n") { (k, v) -> "$k=$v" }

    private fun decode(raw: String?): Settings {
        val d = Settings()
        val m = raw.orEmpty().lines().mapNotNull { line ->
            line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
        }.toMap()
        fun b(k: String, def: Boolean) = m[k]?.toBooleanStrictOrNull() ?: def
        fun i(k: String, def: Int) = m[k]?.toIntOrNull() ?: def
        fun f(k: String, def: Float) = m[k]?.toFloatOrNull() ?: def
        return Settings(
            diagonalButtonsEnabled = b("diagonalButtonsEnabled", d.diagonalButtonsEnabled),
            gestureControlsEnabled = b("gestureControlsEnabled", d.gestureControlsEnabled),
            onScreenButtonsEnabled = b("onScreenButtonsEnabled", d.onScreenButtonsEnabled),
            startingDifficulty = i("startingDifficulty", d.startingDifficulty),
            portraitButtonHeight = f("portraitButtonHeight", d.portraitButtonHeight),
            soundEnabled = b("soundEnabled", d.soundEnabled),
            leftHandedMode = b("leftHandedMode", d.leftHandedMode),
            blockSet = m["blockSet"]?.let { n -> BlockSet.entries.firstOrNull { it.name == n } } ?: d.blockSet,
            wellSize = i("wellSize", d.wellSize),
            wellHeight = i("wellHeight", d.wellHeight),
            buttonOpacity = f("buttonOpacity", d.buttonOpacity),
            buttonScale = f("buttonScale", d.buttonScale),
            portraitButtonInset = f("portraitButtonInset", d.portraitButtonInset),
            landscapeButtonInset = f("landscapeButtonInset", d.landscapeButtonInset),
            landscapeButtonHeight = f("landscapeButtonHeight", d.landscapeButtonHeight),
        )
    }
}

private object LocalStorageHighScores : HighScoreRepository {
    private const val KEY = "blokhead.highScores"
    override val entries = MutableStateFlow(HighScoreCodec.decode(readStorage(KEY).orEmpty()))

    override suspend fun submit(name: String, score: Int): List<HighScoreEntry> {
        val updated = HighScoreTable.insert(entries.value, name, score)
        writeStorage(KEY, HighScoreCodec.encode(updated))
        entries.value = updated
        return updated
    }
}

/** "Action=keycode" lines; -1 means explicitly unbound, as in the Android store. */
private object LocalStorageGamepadBindings : GamepadBindingsRepository {
    private const val KEY = "blokhead.gamepadBindings"
    private const val UNBOUND = -1
    override val bindings: MutableStateFlow<GamepadBindings> = MutableStateFlow(decode(readStorage(KEY)))

    override suspend fun save(bindings: GamepadBindings) {
        writeStorage(KEY, GamepadAction.entries.joinToString("\n") { "${it.name}=${bindings.keyCodes[it] ?: UNBOUND}" })
        this.bindings.value = bindings
    }

    private fun decode(raw: String?): GamepadBindings {
        val defaults = GamepadBindings()
        if (raw == null) return defaults
        val stored = raw.lines().mapNotNull { line ->
            line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1].toIntOrNull() }
        }.toMap()
        return GamepadBindings(GamepadAction.entries.associateWith { action ->
            when (val code = stored[action.name]) {
                null -> defaults.keyCodes[action]
                UNBOUND -> null
                else -> code
            }
        })
    }
}

