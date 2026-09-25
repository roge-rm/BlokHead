package com.rm.blokhead.game

/** The Android KeyEvent keycodes BlokHead's gamepad bindings are stored as, as plain constants so
 *  shared code can name them without android.view.KeyEvent. Values are Android's own, so bindings
 *  saved before this existed still mean the same buttons; the browser build maps the W3C Gamepad
 *  API's standard buttons onto these same numbers (see wasmJsMain's WebGamepad). KeycodesTest in
 *  :app checks each one against KeyEvent. */
object Keycodes {
    const val DPAD_UP = 19
    const val DPAD_DOWN = 20
    const val DPAD_LEFT = 21
    const val DPAD_RIGHT = 22
    const val BUTTON_A = 96
    const val BUTTON_B = 97
    const val BUTTON_X = 99
    const val BUTTON_Y = 100
    const val BUTTON_L1 = 102
    const val BUTTON_R1 = 103
    const val BUTTON_L2 = 104
    const val BUTTON_R2 = 105
    const val BUTTON_THUMBL = 106
    const val BUTTON_THUMBR = 107
    const val BUTTON_START = 108
    const val BUTTON_SELECT = 109
}
