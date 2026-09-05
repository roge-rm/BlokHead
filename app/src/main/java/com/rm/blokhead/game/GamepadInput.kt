package com.rm.blokhead.game

import android.view.KeyEvent
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.data.GamepadBindings

/** Pure resolver: which [GamepadAction] (if any) [keyCode] triggers under [bindings]. The single
 *  seam between a raw captured KeyEvent and gameplay — kept free of Android/Compose dependencies
 *  (beyond the [KeyEvent] keycode constants themselves) so it's directly unit-testable. */
fun resolveGamepadAction(keyCode: Int, bindings: Map<GamepadAction, Int?>): GamepadAction? =
    bindings.entries.firstOrNull { it.value == keyCode }?.key

/**
 * Binds [action] to [keyCode], clearing it from whichever other action currently holds that
 * keycode (a keycode maps to at most one action at a time — otherwise [resolveGamepadAction]
 * would be ambiguous) and returning that bumped action, if any, so the UI can surface it.
 *
 * A/B are ordinary bindable buttons like any other, including in the default bindings — menu
 * Confirm/Back stay reachable through them regardless of what's bound, since `GameScreen` stops
 * resolving gameplay actions altogether the moment its own Confirm/Back affordances (the paused
 * overlay, the exit-confirm dialog) are on screen, no matter which button a player has since
 * assigned to a rotation.
 */
fun reassignBinding(bindings: GamepadBindings, action: GamepadAction, keyCode: Int): Pair<GamepadBindings, GamepadAction?> {
    val bumped = bindings.keyCodes.entries.firstOrNull { it.value == keyCode && it.key != action }?.key
    val updated = bindings.keyCodes.mapValues { (candidate, code) -> if (candidate == bumped) null else code } +
        (action to keyCode)
    return bindings.copy(keyCodes = updated) to bumped
}

/** Short human-readable name for a bound keycode, used by the remapping screen. Covers every
 *  keycode in [com.rm.blokhead.data.defaultGamepadBindings] plus the reserved A/B buttons and a
 *  couple of common extras (Select, right stick) a user might still rebind to manually. */
fun keycodeDisplayName(keyCode: Int?): String = when (keyCode) {
    null -> "Unbound"
    KeyEvent.KEYCODE_DPAD_UP -> "D-Pad Up"
    KeyEvent.KEYCODE_DPAD_DOWN -> "D-Pad Down"
    KeyEvent.KEYCODE_DPAD_LEFT -> "D-Pad Left"
    KeyEvent.KEYCODE_DPAD_RIGHT -> "D-Pad Right"
    KeyEvent.KEYCODE_BUTTON_A -> "A"
    KeyEvent.KEYCODE_BUTTON_B -> "B"
    KeyEvent.KEYCODE_BUTTON_X -> "X"
    KeyEvent.KEYCODE_BUTTON_Y -> "Y"
    KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
    KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
    KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
    KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
    KeyEvent.KEYCODE_BUTTON_THUMBL -> "Left Stick Click"
    KeyEvent.KEYCODE_BUTTON_THUMBR -> "Right Stick Click"
    KeyEvent.KEYCODE_BUTTON_START -> "Start"
    KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
    else -> "Button $keyCode"
}
