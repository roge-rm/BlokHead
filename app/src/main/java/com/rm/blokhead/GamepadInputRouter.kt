package com.rm.blokhead

import android.view.InputDevice
import android.view.KeyEvent

/**
 * Bridges gamepad [KeyEvent]s captured at the Activity level ([MainActivity.dispatchKeyEvent])
 * into whichever part of the Compose tree currently cares about them. A plain mutable holder —
 * matching this codebase's existing preference for threading state via plain objects/composable
 * parameters over DI/ViewModel — since [MainActivity]'s Activity-level override can't otherwise
 * reach state that lives inside a composable's own `remember`.
 *
 * Composables install/clear these fields as they enter/leave composition (`DisposableEffect`),
 * so exactly one of [gameplayHandler]/[captureHandler] is meaningfully active at a time in
 * practice, and [backHandler] tracks whichever non-gameplay screen is currently shown.
 */
class GamepadInputRouter {

    /** Non-null only while a game session is actively playable (i.e. no dialog/overlay is
     *  showing over it) — resolves a gamepad KeyEvent into a `GameEngine` call. Returns whether
     *  it handled the event; `false` means "not gameplay's business right now, defer to Back". */
    var gameplayHandler: ((KeyEvent) -> Boolean)? = null

    /** Non-null only while the controller-remapping screen is waiting for the next button press
     *  to bind to an action. Takes priority over everything else: while listening, every button
     *  (including A/B) is being captured for evaluation, not treated as navigation. */
    var captureHandler: ((keyCode: Int) -> Unit)? = null

    /** Invoked when the fixed "B" button is pressed outside active gameplay/capture-listening —
     *  set by whichever screen is currently shown to its own back/dismiss action. */
    var backHandler: (() -> Unit)? = null

    /**
     * Decides what a gamepad [event] should do. Returns `true` if it was handled (caller should
     * consume it), or `null` if the caller should fall through to the normal Android/Compose key
     * dispatch — e.g. D-pad focus traversal, or [ui.GamepadFocus]'s A-button activation, both of
     * which only work if events reach Compose's own key handling.
     */
    fun handle(event: KeyEvent): Boolean? {
        if (event.action != KeyEvent.ACTION_DOWN || !isGamepadEvent(event)) return null
        captureHandler?.let {
            it(event.keyCode)
            return true
        }
        gameplayHandler?.let { handler ->
            if (handler(event)) return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_B) {
            backHandler?.let {
                it()
                return true
            }
        }
        return null
    }

    companion object {
        private const val GAMEPAD_SOURCES = InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD

        fun isGamepadEvent(event: KeyEvent): Boolean {
            val sources = event.device?.sources ?: return false
            return sources and GAMEPAD_SOURCES != 0
        }
    }
}
