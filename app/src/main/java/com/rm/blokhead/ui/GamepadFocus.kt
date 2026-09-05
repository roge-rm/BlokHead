package com.rm.blokhead.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import android.view.KeyEvent as AndroidKeyEvent

/**
 * Makes an element gamepad-navigable: focusable (so D-pad traversal, already handled for free by
 * Compose's default focus system once a gamepad KeyEvent isn't intercepted earlier, can land on
 * it), a visible focus ring (Material3's default focus indication is subtle against this app's
 * dark theme), and activation on the gamepad's fixed "A" button — Compose Foundation's built-in
 * `clickable` only wires up Enter/DPad-Center to trigger a click, not `KEYCODE_BUTTON_A`, and
 * there's no public API to invoke a click on the currently-focused node from one global listener,
 * so every gamepad-activatable element opts in individually via this modifier.
 */
fun Modifier.gamepadFocusable(onActivate: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    this
        .focusable(interactionSource = interactionSource)
        .then(
            if (isFocused) {
                Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary))
            } else {
                Modifier
            },
        )
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BUTTON_A) {
                onActivate()
                true
            } else {
                false
            }
        }
}
