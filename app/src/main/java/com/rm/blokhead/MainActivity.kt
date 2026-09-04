package com.rm.blokhead

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
    var screen by remember { mutableStateOf(AppScreen.MENU) }
    // Bumped each "Start Game", so GameScreen's remember(sessionId) below starts a fresh
    // GameEngine/GLSurfaceView per playthrough instead of reusing one across menu visits.
    var gameSessionId by remember { mutableIntStateOf(0) }

    when (screen) {
        AppScreen.MENU -> MenuScreen(
            onStartGame = {
                gameSessionId++
                screen = AppScreen.GAME
            },
            onShowHighScores = { screen = AppScreen.HIGH_SCORES },
        )

        AppScreen.GAME -> GameScreen(
            sessionId = gameSessionId,
            highScoreStore = highScoreStore,
            onExitToMenu = { screen = AppScreen.MENU },
        )

        AppScreen.HIGH_SCORES -> {
            var entries by remember { mutableStateOf(emptyList<HighScoreEntry>()) }
            LaunchedEffect(Unit) { highScoreStore.entries.collect { entries = it } }
            HighScoreScreen(entries = entries, onBack = { screen = AppScreen.MENU })
        }
    }
}

@Composable
private fun GameScreen(sessionId: Int, highScoreStore: HighScoreStore, onExitToMenu: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val engine = remember(sessionId) { GameEngine() }
    val surfaceView = remember(sessionId) { BlokoutSurfaceView(context, engine) }

    // GameEngine's mutable state is owned by the GL thread (see BlokoutRenderer); this polls the
    // plain Int/Boolean fields on a timer for display rather than wiring a proper state stream,
    // since a HUD is fine lagging one tick behind and this avoids adding cross-thread
    // synchronization to the ported game logic just for readouts.
    var hud by remember(sessionId) { mutableStateOf(HudSnapshot(0, 0, 0, false)) }
    LaunchedEffect(engine) {
        while (true) {
            hud = HudSnapshot(engine.score, engine.level, engine.cubesDropped, engine.isGameOver)
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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { surfaceView })

        GameHud(
            snapshot = hud,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
        )

        GameControls(
            onMove = { axis, sign ->
                surfaceView.enqueue {
                    when (axis) {
                        Axis.X -> if (sign < 0) moveLeft() else moveRight()
                        else -> if (sign < 0) moveBackward() else moveForward()
                    }
                }
            },
            onRotate = { axis, sign -> surfaceView.enqueue { rotate(axis, sign) } },
            onHardDrop = { surfaceView.enqueue { hardDrop() } },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )

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
