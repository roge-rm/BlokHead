package com.rm.blokhead

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rm.blokhead.audio.SfxPlayer
import com.rm.blokhead.data.HighScoreEntry
import com.rm.blokhead.data.HighScoreStore
import com.rm.blokhead.game.Axis
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.render.BlokoutSurfaceView
import com.rm.blokhead.ui.AppScreen
import com.rm.blokhead.ui.GameControls
import com.rm.blokhead.ui.GameHud
import com.rm.blokhead.ui.GameOverOverlay
import com.rm.blokhead.ui.HighScoreScreen
import com.rm.blokhead.ui.HudSnapshot
import com.rm.blokhead.ui.MenuScreen
import com.rm.blokhead.ui.NameEntryOverlay
import com.rm.blokhead.ui.PausedOverlay
import com.rm.blokhead.ui.theme.BlokHeadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlokHeadTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlokHeadApp()
                }
            }
        }
    }
}

@Composable
private fun BlokHeadApp() {
    val context = LocalContext.current
    val highScoreStore = remember { HighScoreStore(context.applicationContext) }
    val sfx = remember { SfxPlayer(context.applicationContext) }
    DisposableEffect(sfx) { onDispose { sfx.release() } }

    var screen by remember { mutableStateOf(AppScreen.MENU) }
    // Bumped each "Start Game", so GameScreen's remember(sessionId) below starts a fresh
    // GameEngine/GLSurfaceView per playthrough instead of reusing one across menu visits.
    var gameSessionId by remember { mutableIntStateOf(0) }

    when (screen) {
        AppScreen.MENU -> MenuScreen(
            onStartGame = {
                sfx.playMenu()
                gameSessionId++
                screen = AppScreen.GAME
            },
            onShowHighScores = {
                sfx.playMenu()
                screen = AppScreen.HIGH_SCORES
            },
        )

        AppScreen.GAME -> GameScreen(
            sessionId = gameSessionId,
            highScoreStore = highScoreStore,
            sfx = sfx,
            onExitToMenu = { screen = AppScreen.MENU },
        )

        AppScreen.HIGH_SCORES -> {
            var entries by remember { mutableStateOf(emptyList<HighScoreEntry>()) }
            LaunchedEffect(Unit) { highScoreStore.entries.collect { entries = it } }
            HighScoreScreen(entries = entries, onBack = { sfx.playMenu(); screen = AppScreen.MENU })
        }
    }
}

@Composable
private fun GameScreen(sessionId: Int, highScoreStore: HighScoreStore, sfx: SfxPlayer, onExitToMenu: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val engine = remember(sessionId) { GameEngine() }
    val surfaceView = remember(sessionId) { BlokoutSurfaceView(context, engine) }

    // GameEngine's mutable state is owned by the GL thread (see BlokoutRenderer); this polls the
    // plain Int/Boolean fields on a timer for display rather than wiring a proper state stream,
    // since a HUD is fine lagging one tick behind and this avoids adding cross-thread
    // synchronization to the ported game logic just for readouts. The same poll also diffs
    // cubesDropped/levelsDescended/isGameOver against their previous values to trigger sound
    // effects for lock/layer-clear/game-over — those are engine-state transitions, not direct
    // taps, so there's no single call site to hang a sfx.play...() off of otherwise.
    var hud by remember(sessionId) { mutableStateOf(HudSnapshot(0, 0, 0, false, false)) }
    LaunchedEffect(engine) {
        var previousCubesDropped = engine.cubesDropped
        var previousLayersCleared = engine.levelsDescended
        var previousGameOver = engine.isGameOver
        while (true) {
            val cubesDropped = engine.cubesDropped
            val layersCleared = engine.levelsDescended
            val gameOver = engine.isGameOver
            if (cubesDropped != previousCubesDropped) sfx.playLock()
            if (layersCleared != previousLayersCleared) sfx.playClear()
            if (gameOver && !previousGameOver) sfx.playGameOver()
            previousCubesDropped = cubesDropped
            previousLayersCleared = layersCleared
            previousGameOver = gameOver

            hud = HudSnapshot(engine.score, engine.level, cubesDropped, gameOver, engine.isPaused)
            delay(100)
        }
    }

    // Once game-over fires, check the high-score table exactly once for this playthrough: either
    // the score qualifies (show NameEntryOverlay) or it doesn't (go straight to GameOverOverlay).
    var highScoreQualified by remember(sessionId) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(hud.isGameOver) {
        if (hud.isGameOver && highScoreQualified == null) {
            highScoreQualified = highScoreStore.isHighScore(hud.score)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { surfaceView })

        GameHud(snapshot = hud, modifier = Modifier.fillMaxWidth())

        // BlokoutRenderer's camera solves the vertical FOV so the well's near opening exactly
        // fills the viewport width; since the well is square (width == depth), that makes the
        // opening's projected height a fixed `aspect` fraction of the screen height, centered —
        // i.e. the rendered grid occupies the vertical band [0.5 - aspect/2, 0.5 + aspect/2].
        val containerHeight = maxHeight
        val aspect = maxWidth.value / maxHeight.value
        val gridBottomFraction = 0.5f + aspect / 2f
        val gridTopHeight = containerHeight * (0.5f - aspect / 2f)

        // Tapping anywhere above the grid (the HUD's band included) pauses/unpauses — placed
        // before the controls/overlays below so it never steals taps meant for them, and it's
        // a no-op once the game has ended (pausing a finished game doesn't mean anything).
        if (!hud.isGameOver) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridTopHeight)
                    .pointerInput(engine) {
                        detectTapGestures {
                            surfaceView.enqueue { setPaused(!isPaused) }
                        }
                    },
            )
        }

        // Placing the controls right after the grid's bottom edge (plus a small gap) keeps them
        // just clear of it regardless of screen size, rather than at a fixed guessed position.
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(containerHeight * gridBottomFraction + 12.dp))
            GameControls(
                onMove = { axis, sign ->
                    sfx.playMove()
                    surfaceView.enqueue {
                        when (axis) {
                            Axis.X -> if (sign < 0) moveLeft() else moveRight()
                            else -> if (sign < 0) moveBackward() else moveForward()
                        }
                    }
                },
                onRotate = { axis, sign ->
                    sfx.playRotate()
                    surfaceView.enqueue { rotate(axis, sign) }
                },
                onHardDrop = { surfaceView.enqueue { hardDrop() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }

        AnimatedVisibility(
            visible = hud.isPaused && !hud.isGameOver,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            PausedOverlay()
        }

        if (hud.isGameOver) {
            if (highScoreQualified == true) {
                NameEntryOverlay(
                    finalScore = hud.score,
                    onSubmit = { name ->
                        coroutineScope.launch {
                            highScoreStore.submit(name, hud.score)
                            highScoreQualified = false // falls through to GameOverOverlay below
                        }
                    },
                )
            } else if (highScoreQualified == false) {
                GameOverOverlay(
                    finalScore = hud.score,
                    onPlayAgain = {
                        highScoreQualified = null
                        surfaceView.enqueue { restart() }
                    },
                    onMainMenu = onExitToMenu,
                )
            }
        }
    }
}
