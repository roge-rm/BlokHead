package com.rm.blokhead.game

import android.view.KeyEvent
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.data.GamepadBindings

/** Pure resolver: which [GamepadAction] (if any) [keyCode] triggers under [bindings]. The single
 *  seam between a raw captured KeyEvent and gameplay — kept free of Android/Compose dependencies
 *  (beyond the [KeyEvent] keycode constants themselves) so it's directly unit-testable. */
fun resolveGamepadAction(keyCode: Int, bindings: Map<GamepadAction, Int?>): GamepadAction? =
    bindings.entries.firstOrNull { it.value == keyCode }?.key

/** True for the two buttons permanently reserved for menu Confirm/Back — never assignable to a
 *  gameplay action, regardless of remapping. */
fun isReservedForMenus(keyCode: Int): Boolean =
    keyCode == KeyEvent.KEYCODE_BUTTON_A || keyCode == KeyEvent.KEYCODE_BUTTON_B

/**
 * Binds [action] to [keyCode], clearing it from whichever other action currently holds that
 * keycode (a keycode maps to at most one action at a time — otherwise [resolveGamepadAction]
 * would be ambiguous) and returning that bumped action, if any, so the UI can surface it.
 *
 * Rejects an attempt to bind a menu-reserved button: returns [bindings] unchanged and a null
 * bumped action, so a saved binding can never collide with Confirm/Back — enforced here, not
 * only in the UI, so it holds regardless of how many call sites ever trigger a rebind.
 */
fun reassignBinding(bindings: GamepadBindings, action: GamepadAction, keyCode: Int): Pair<GamepadBindings, GamepadAction?> {
    if (isReservedForMenus(keyCode)) return bindings to null
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
