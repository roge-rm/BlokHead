package com.rm.blokhead

import android.view.InputDevice
import android.view.KeyEvent

private const val GAMEPAD_SOURCES = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD

/** Android's half of [GamepadInputRouter]: only key-downs from a gamepad/joystick/D-pad device go
 *  to the shared router, everything else (and anything it declines) falls through to the normal
 *  Android/Compose key dispatch. */
fun GamepadInputRouter.handle(event: KeyEvent): Boolean? {
    if (event.action != KeyEvent.ACTION_DOWN || !isGamepadEvent(event)) return null
    return handleButtonDown(event.keyCode)
}

private fun isGamepadEvent(event: KeyEvent): Boolean {
    val sources = event.device?.sources ?: return false
    return sources and GAMEPAD_SOURCES != 0
}
