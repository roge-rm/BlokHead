package com.rm.blokhead.game

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

/** Stored gamepad bindings are Android keycodes; [Keycodes] must keep matching them. */
class KeycodesTest {
    @Test
    fun keycodesMatchAndroid() {
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, Keycodes.DPAD_UP)
        assertEquals(KeyEvent.KEYCODE_DPAD_DOWN, Keycodes.DPAD_DOWN)
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, Keycodes.DPAD_LEFT)
        assertEquals(KeyEvent.KEYCODE_DPAD_RIGHT, Keycodes.DPAD_RIGHT)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, Keycodes.BUTTON_A)
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, Keycodes.BUTTON_B)
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, Keycodes.BUTTON_X)
        assertEquals(KeyEvent.KEYCODE_BUTTON_Y, Keycodes.BUTTON_Y)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L1, Keycodes.BUTTON_L1)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R1, Keycodes.BUTTON_R1)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, Keycodes.BUTTON_L2)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, Keycodes.BUTTON_R2)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, Keycodes.BUTTON_THUMBL)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBR, Keycodes.BUTTON_THUMBR)
        assertEquals(KeyEvent.KEYCODE_BUTTON_START, Keycodes.BUTTON_START)
        assertEquals(KeyEvent.KEYCODE_BUTTON_SELECT, Keycodes.BUTTON_SELECT)
    }
}
