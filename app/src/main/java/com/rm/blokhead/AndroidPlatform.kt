package com.rm.blokhead

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rm.blokhead.audio.Sfx
import com.rm.blokhead.audio.SfxPlayer
import com.rm.blokhead.data.GamepadBindingsRepository
import com.rm.blokhead.data.GamepadBindingsStore
import com.rm.blokhead.data.HighScoreRepository
import com.rm.blokhead.data.HighScoreStore
import com.rm.blokhead.data.SettingsRepository
import com.rm.blokhead.data.SettingsStore
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.render.BlokoutSurfaceView

/** [BlokHeadPlatform] for the Android app: DataStore, SoundPool and the GLES renderer. */
class AndroidPlatform(private val appContext: Context) : BlokHeadPlatform {
    override val versionName: String = BuildConfig.VERSION_NAME

    override fun createSettingsRepository(): SettingsRepository = SettingsStore(appContext)
    override fun createHighScoreRepository(): HighScoreRepository = HighScoreStore(appContext)
    override fun createGamepadBindingsRepository(): GamepadBindingsRepository = GamepadBindingsStore(appContext)
    override fun createSfx(): Sfx = SfxPlayer(appContext)

    @Composable
    override fun rememberGameSurface(sessionId: Int, engine: GameEngine): GameSurface {
        val context = LocalContext.current
        val surfaceView = remember(sessionId) { BlokoutSurfaceView(context, engine) }

        // Nothing else in this app matches the Activity's own lifecycle to the game/GL surface's —
        // without this, backgrounding only *looks* paused because Android happens to tear down the
        // GL surface for an invisible window (stopping onDrawFrame, which is what drives
        // engine.update()); that's incidental, not guaranteed (multi-window/some launchers' recent-
        // apps preview can keep the surface alive), and GLSurfaceView's own docs call for onPause()/
        // onResume() regardless. ON_STOP pauses the game the same way tapping the grid does (so it
        // resumes exactly where it was, PAUSED overlay and all, never auto-unpausing on return) and
        // suspends the GL thread; ON_START only resumes the GL thread, leaving the game paused until
        // the player explicitly taps to continue.
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner, surfaceView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        surfaceView.enqueue { setPaused(true) }
                        surfaceView.onPause()
                    }
                    Lifecycle.Event.ON_START -> surfaceView.onResume()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        return remember(surfaceView) {
            object : GameSurface {
                override fun enqueue(action: GameEngine.() -> Unit) = surfaceView.enqueue(action)

                @Composable
                override fun Content(modifier: Modifier) {
                    AndroidView(modifier = modifier, factory = { surfaceView })
                }
            }
        }
    }

    @Composable
    override fun BackHandler(enabled: Boolean, onBack: () -> Unit) =
        androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)

    @Composable
    override fun isLandscape(): Boolean =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
}
