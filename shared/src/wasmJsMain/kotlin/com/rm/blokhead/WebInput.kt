package com.rm.blokhead

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.game.Keycodes
import kotlinx.browser.document
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/** Keyboard controls for the browser build, by physical key position (KeyboardEvent.code), so they
 *  sit in the same place on any layout. Rotations follow the original Blockout's pairs: Q/A, W/S
 *  and E/D turn about X, Y and Z. */
private val KEYBOARD_ACTIONS = mapOf(
    "ArrowLeft" to GamepadAction.MoveLeft,
    "ArrowRight" to GamepadAction.MoveRight,
    "ArrowUp" to GamepadAction.MoveForward,
    "ArrowDown" to GamepadAction.MoveBackward,
    "KeyQ" to GamepadAction.RotateXPositive,
    "KeyA" to GamepadAction.RotateXNegative,
    "KeyW" to GamepadAction.RotateYPositive,
    "KeyS" to GamepadAction.RotateYNegative,
    "KeyE" to GamepadAction.RotateZPositive,
    "KeyD" to GamepadAction.RotateZNegative,
    "Space" to GamepadAction.HardDrop,
    "KeyP" to GamepadAction.Pause,
)

/** One line per control, for the page and the About screen. */
const val KEYBOARD_HELP = "Arrows move · Q/A, W/S, E/D rotate · Space drops · P pauses · Esc goes back"

/** Keyboard input goes straight to game actions ([GamepadInputRouter.actionHandler]), not through
 *  the gamepad bindings; Esc does what Android's Back button does. Keys the game doesn't use (or
 *  can't use right now, e.g. while typing a high-score name) are left alone. */
fun installKeyboardControls(router: GamepadInputRouter) {
    document.addEventListener("keydown", { event: Event ->
        val key = event.unsafeCast<KeyboardEvent>()
        if (key.repeat) return@addEventListener
        val handled = when (key.code) {
            "Escape" -> router.backHandler?.let { it(); true } ?: false
            else -> KEYBOARD_ACTIONS[key.code]?.let { router.actionHandler?.invoke(it) } ?: false
        }
        if (handled) event.preventDefault()
    })
}

/** Standard-mapping Gamepad API button indices, as the Android keycodes bindings are stored in. */
private val GAMEPAD_BUTTON_KEYCODES = mapOf(
    0 to Keycodes.BUTTON_A,
    1 to Keycodes.BUTTON_B,
    2 to Keycodes.BUTTON_X,
    3 to Keycodes.BUTTON_Y,
    4 to Keycodes.BUTTON_L1,
    5 to Keycodes.BUTTON_R1,
    6 to Keycodes.BUTTON_L2,
    7 to Keycodes.BUTTON_R2,
    8 to Keycodes.BUTTON_SELECT,
    9 to Keycodes.BUTTON_START,
    10 to Keycodes.BUTTON_THUMBL,
    11 to Keycodes.BUTTON_THUMBR,
    12 to Keycodes.DPAD_UP,
    13 to Keycodes.DPAD_DOWN,
    14 to Keycodes.DPAD_LEFT,
    15 to Keycodes.DPAD_RIGHT,
)

/** The Gamepad API has no button events, only state to poll: this checks once a frame and feeds
 *  each newly pressed button to [router], like Android's dispatchKeyEvent does. Gameplay, Back
 *  and the remapping screen work; unlike Android, the D-pad doesn't move focus around menus. */
@Composable
fun GamepadPoller(router: GamepadInputRouter) {
    LaunchedEffect(router) {
        var previous = 0
        while (true) {
            withFrameNanos {
                val pressed = pressedGamepadButtons()
                val newlyPressed = pressed and previous.inv()
                previous = pressed
                if (newlyPressed != 0) {
                    for ((bit, keyCode) in GAMEPAD_BUTTON_KEYCODES) {
                        if (newlyPressed and (1 shl bit) != 0) router.handleButtonDown(keyCode)
                    }
                }
            }
        }
    }
}

/** Bit n set = standard button n held on any connected standard-mapping gamepad. */
private fun pressedGamepadButtons(): Int = js(
    """(() => {
        if (!navigator.getGamepads) return 0;
        let mask = 0;
        for (const pad of navigator.getGamepads()) {
            if (!pad || pad.mapping !== 'standard') continue;
            pad.buttons.forEach((b, i) => { if (b.pressed && i < 31) mask |= (1 << i); });
        }
        return mask;
    })()"""
)
