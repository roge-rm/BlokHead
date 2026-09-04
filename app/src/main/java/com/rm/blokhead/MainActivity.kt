package com.rm.blokhead

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.render.BlokoutSurfaceView
import com.rm.blokhead.ui.theme.BlokHeadTheme

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

// HUD/menu overlays land in a later pass; for now this just hosts the GL well/renderer.
@Composable
private fun GameScreen() {
    val context = LocalContext.current
    val engine = remember { GameEngine() }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { BlokoutSurfaceView(context, engine) },
    )
}
