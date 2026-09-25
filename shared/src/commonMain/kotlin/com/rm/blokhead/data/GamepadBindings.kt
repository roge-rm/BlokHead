package com.rm.blokhead.data

import com.rm.blokhead.game.Keycodes

/** Every gameplay action a gamepad button can be bound to. Diagonal moves aren't here — they're
 *  a touch-only convenience combining two of these — and menu Confirm/Back aren't either: those
 *  are permanently fixed to the A/B buttons regardless of what (if anything) a player also binds
 *  those buttons to below — see `GameScreen` in BlokHeadApp.kt, which stops
 *  resolving gameplay actions at all (bound button or not) the moment the paused overlay's own
 *  Confirm/Back affordances are on screen. */
enum class GamepadAction {
    MoveLeft, MoveRight, MoveForward, MoveBackward,
    RotateXPositive, RotateXNegative, RotateYPositive, RotateYNegative,
    RotateZPositive, RotateZNegative, HardDrop, Pause,
}

/** [keyCodes] maps every [GamepadAction] to the KeyEvent keycode that triggers it, or null if
 *  unbound. A keycode maps to at most one action at a time — see
 *  [com.rm.blokhead.game.reassignBinding], the only place bindings should be mutated. */
data class GamepadBindings(val keyCodes: Map<GamepadAction, Int?> = defaultGamepadBindings())

/** Tuned against a standard Bluetooth/Xbox-layout controller: the four face buttons drive the two
 *  most-used rotations (X on X+/-, Y on Y+/-) rather than being left to the reserved-feeling A/B
 *  pair, with the bumpers taking the remaining Z rotation. */
fun defaultGamepadBindings(): Map<GamepadAction, Int?> = mapOf(
    GamepadAction.MoveLeft to Keycodes.DPAD_LEFT,
    GamepadAction.MoveRight to Keycodes.DPAD_RIGHT,
    GamepadAction.MoveForward to Keycodes.DPAD_UP,
    GamepadAction.MoveBackward to Keycodes.DPAD_DOWN,
    GamepadAction.RotateXPositive to Keycodes.BUTTON_A,
    GamepadAction.RotateXNegative to Keycodes.BUTTON_Y,
    GamepadAction.RotateYPositive to Keycodes.BUTTON_X,
    GamepadAction.RotateYNegative to Keycodes.BUTTON_B,
    GamepadAction.RotateZPositive to Keycodes.BUTTON_R1,
    GamepadAction.RotateZNegative to Keycodes.BUTTON_L1,
    GamepadAction.HardDrop to Keycodes.BUTTON_THUMBL,
    GamepadAction.Pause to Keycodes.BUTTON_START,
)
