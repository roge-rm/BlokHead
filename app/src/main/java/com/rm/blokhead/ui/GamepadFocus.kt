package com.rm.blokhead.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import android.view.KeyEvent as AndroidKeyEvent

/**
 * Android auto-assigns initial focus to the first focusable view in a window whenever it isn't in
 * touch mode (e.g. a gamepad is connected at launch) — so without this gate, a screen's first
 * gamepad-focusable element shows its focus ring immediately, even if the player never touches
 * the controller. [markInputReceived] is called from [GamepadInputRouter] the moment a real
 * gamepad KeyEvent arrives, and every [gamepadFocusable] ring stays hidden until then.
 */
object GamepadFocusVisibility {
    var hasReceivedInput by mutableStateOf(false)
        private set

    fun markInputReceived() {
        hasReceivedInput = true
    }
}

/**
 * Makes an element gamepad-navigable: focusable (so D-pad traversal, already handled for free by
 * Compose's default focus system once a gamepad KeyEvent isn't intercepted earlier, can land on
 * it), a visible focus ring (Material3's default focus indication is subtle against this app's
 * dark theme), and activation on the gamepad's fixed "A" button — Compose Foundation's built-in
 * `clickable` only wires up Enter/DPad-Center to trigger a click, not `KEYCODE_BUTTON_A`, and
 * there's no public API to invoke a click on the currently-focused node from one global listener,
 * so every gamepad-activatable element opts in individually via this modifier.
 *
 * [shape] defaults to the standard M3 button shape (a full stadium/pill, shared by `Button`/
 * `OutlinedButton`/`TextButton` alike) since that's what most callers wrap; pass the caller's own
 * shape (e.g. a row clipped to `RoundedCornerShape(8.dp)`) so the ring outlines it exactly instead
 * of drawing a plain rectangle over its rounded/pill corners.
 */
fun Modifier.gamepadFocusable(shape: Shape? = null, onActivate: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    this
        .focusable(interactionSource = interactionSource)
        .then(
            if (isFocused && GamepadFocusVisibility.hasReceivedInput) {
                Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape ?: ButtonDefaults.shape)
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
