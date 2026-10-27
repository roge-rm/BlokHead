package com.rm.blokhead

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import com.rm.blokhead.render.wellBackgroundColor
import com.rm.blokhead.ui.theme.BlokHeadTheme
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val gamepadRouter = GamepadInputRouter()
    installKeyboardControls(gamepadRouter)
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        BlokHeadTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = wellBackgroundColor) {
                BlokHeadApp(WebPlatform, gamepadRouter)
            }
        }
        GamepadPoller(gamepadRouter)
    }
}
