package com.rm.blokhead.game

import android.view.KeyEvent
import com.rm.blokhead.data.GamepadAction
import com.rm.blokhead.data.GamepadBindings
import com.rm.blokhead.data.defaultGamepadBindings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GamepadInputTest {

    @Test
    fun `resolveGamepadAction finds the action bound to a keycode`() {
        val bindings = mapOf(GamepadAction.HardDrop to KeyEvent.KEYCODE_BUTTON_THUMBL)
        assertEquals(GamepadAction.HardDrop, resolveGamepadAction(KeyEvent.KEYCODE_BUTTON_THUMBL, bindings))
    }

    @Test
    fun `resolveGamepadAction returns null for an unbound keycode`() {
        val bindings = mapOf(GamepadAction.HardDrop to KeyEvent.KEYCODE_BUTTON_THUMBL)
        assertNull(resolveGamepadAction(KeyEvent.KEYCODE_BUTTON_START, bindings))
    }

    @Test
    fun `resolveGamepadAction returns null for an empty map`() {
        assertNull(resolveGamepadAction(KeyEvent.KEYCODE_DPAD_UP, emptyMap()))
    }

    @Test
    fun `reassignBinding sets a fresh binding`() {
        val bindings = GamepadBindings(keyCodes = emptyMap())
        val (updated, bumped) = reassignBinding(bindings, GamepadAction.HardDrop, KeyEvent.KEYCODE_BUTTON_THUMBL)
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, updated.keyCodes[GamepadAction.HardDrop])
        assertNull(bumped)
    }

    @Test
    fun `reassignBinding bumps whichever other action held that keycode`() {
        val bindings = GamepadBindings(
            keyCodes = mapOf(
                GamepadAction.HardDrop to KeyEvent.KEYCODE_BUTTON_THUMBL,
                GamepadAction.Pause to KeyEvent.KEYCODE_BUTTON_START,
            ),
        )
        val (updated, bumped) = reassignBinding(bindings, GamepadAction.Pause, KeyEvent.KEYCODE_BUTTON_THUMBL)
        assertEquals(GamepadAction.HardDrop, bumped)
        assertNull(updated.keyCodes[GamepadAction.HardDrop])
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, updated.keyCodes[GamepadAction.Pause])
    }

    @Test
    fun `reassignBinding allows binding an action to A or B`() {
        val bindings = GamepadBindings(keyCodes = emptyMap())
        val (updated, bumped) = reassignBinding(bindings, GamepadAction.HardDrop, KeyEvent.KEYCODE_BUTTON_A)
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, updated.keyCodes[GamepadAction.HardDrop])
        assertNull(bumped)
    }

    @Test
    fun `default bindings cover every action with no collisions`() {
        val defaults = defaultGamepadBindings()
        assertEquals(GamepadAction.entries.toSet(), defaults.keys)

        val boundCodes = defaults.values.filterNotNull()
        assertEquals("no two actions should default to the same button", boundCodes.size, boundCodes.toSet().size)
    }

    @Test
    fun `keycodeDisplayName covers unbound and a few known buttons`() {
        assertEquals("Unbound", keycodeDisplayName(null))
        assertEquals("A", keycodeDisplayName(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals("D-Pad Up", keycodeDisplayName(KeyEvent.KEYCODE_DPAD_UP))
    }
}
