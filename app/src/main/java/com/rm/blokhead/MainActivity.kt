package com.rm.blokhead

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rm.blokhead.ui.theme.BlokHeadTheme

class MainActivity : ComponentActivity() {
    // Activity-level field (not `remember`-ed — a composable can't be reached from
    // dispatchKeyEvent) that every gamepad-aware composable installs/clears itself into as it
    // enters/leaves composition. See GamepadInputRouter's doc.
    private val gamepadRouter = GamepadInputRouter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val platform = AndroidPlatform(applicationContext)
        setContent {
            BlokHeadTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlokHeadApp(platform, gamepadRouter)
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        gamepadRouter.handle(event) ?: super.dispatchKeyEvent(event)
}
