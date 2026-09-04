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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rm.blokhead.game.Axis
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.render.BlokoutSurfaceView
import com.rm.blokhead.ui.GameControls
import com.rm.blokhead.ui.GameHud
import com.rm.blokhead.ui.GameOverOverlay
import com.rm.blokhead.ui.HudSnapshot
import com.rm.blokhead.ui.theme.BlokHeadTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BlokHeadTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
private fun GameScreen() {
    val context = LocalContext.current
    val engine = remember { GameEngine() }
    val surfaceView = remember { BlokoutSurfaceView(context, engine) }

    // GameEngine's mutable state is owned by the GL thread (see BlokoutRenderer); this polls the
    // plain Int/Boolean fields on a timer for display rather than wiring a proper state stream,
    // since a HUD is fine lagging one tick behind and this avoids adding cross-thread
    // synchronization to the ported game logic just for readouts.
    var hud by remember { mutableStateOf(HudSnapshot(0, 0, 0, false)) }
    LaunchedEffect(engine) {
        while (true) {
            hud = HudSnapshot(engine.score, engine.level, engine.cubesDropped, engine.isGameOver)
            delay(100)
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
            GameOverOverlay(
                finalScore = hud.score,
                onPlayAgain = { surfaceView.enqueue { restart() } },
            )
        }
    }
}
