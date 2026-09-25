package com.rm.blokhead

import com.rm.blokhead.game.Keycodes
import com.rm.blokhead.ui.GamepadFocusVisibility

/**
 * Bridges gamepad button presses captured outside Compose (on Android, MainActivity.dispatchKeyEvent;
 * in the browser, the Gamepad API poll in wasmJsMain) into whichever part of the Compose tree
 * currently cares about them. Presses arrive as [Keycodes] values. A plain mutable holder —
 * matching this codebase's existing preference for threading state via plain objects/composable
 * parameters over DI/ViewModel — since an Activity-level override (or a browser poll loop) can't otherwise
 * reach state that lives inside a composable's own `remember`.
 *
 * Composables install/clear these fields as they enter/leave composition (`DisposableEffect`),
 * so exactly one of [gameplayHandler]/[captureHandler] is meaningfully active at a time in
 * practice, and [backHandler] tracks whichever non-gameplay screen is currently shown.
 */
class GamepadInputRouter {

    /** Non-null only while a game session is actively playable (i.e. no dialog/overlay is
     *  showing over it) — resolves a gamepad keycode into a `GameEngine` call. Returns whether
     *  it handled the press; `false` means "not gameplay's business right now, defer to Back". */
    var gameplayHandler: ((keyCode: Int) -> Boolean)? = null

    /** Set alongside [gameplayHandler] for input that names actions directly rather than going
     *  through gamepad bindings — the browser build's keyboard controls. Same return convention. */
    var actionHandler: ((com.rm.blokhead.data.GamepadAction) -> Boolean)? = null

    /** Non-null only while the controller-remapping screen is waiting for the next button press
     *  to bind to an action. Takes priority over everything else: while listening, every button
     *  (including A/B) is being captured for evaluation, not treated as navigation. */
    var captureHandler: ((keyCode: Int) -> Unit)? = null

    /** Invoked when the fixed "B" button is pressed outside active gameplay/capture-listening —
     *  set by whichever screen is currently shown to its own back/dismiss action. */
    var backHandler: (() -> Unit)? = null

    /**
     * Decides what a gamepad button press ([keyCode], already known to come from a gamepad) should
     * do. Returns `true` if it was handled (caller should consume it), or `null` if the caller
     * should fall through to the normal key dispatch — e.g. D-pad focus traversal, or
     * [ui.GamepadFocus]'s A-button activation, both of which only work if events reach Compose's
     * own key handling.
     */
    fun handleButtonDown(keyCode: Int): Boolean? {
        GamepadFocusVisibility.markInputReceived()
        captureHandler?.let {
            it(keyCode)
            return true
        }
        gameplayHandler?.let { handler ->
            if (handler(keyCode)) return true
        }
        if (keyCode == Keycodes.BUTTON_B) {
            backHandler?.let {
                it()
                return true
            }
        }
        return null
    }
}
