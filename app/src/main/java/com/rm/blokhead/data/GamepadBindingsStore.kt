package com.rm.blokhead.data

import android.content.Context
import android.view.KeyEvent
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gamepadBindingsDataStore by preferencesDataStore(name = "gamepad_bindings")

/** Every gameplay action a gamepad button can be bound to. Diagonal moves aren't here — they're
 *  a touch-only convenience combining two of these — and menu Confirm/Back aren't either: those
 *  are permanently fixed to the A/B buttons (see [com.rm.blokhead.game.isReservedForMenus]),
 *  never user-remappable. */
enum class GamepadAction {
    MoveLeft, MoveRight, MoveForward, MoveBackward,
    RotateXPositive, RotateXNegative, RotateYPositive, RotateYNegative,
    RotateZPositive, RotateZNegative, HardDrop, Pause,
}

/** [keyCodes] maps every [GamepadAction] to the KeyEvent keycode that triggers it, or null if
 *  unbound. A keycode maps to at most one action at a time — see
 *  [com.rm.blokhead.game.reassignBinding], the only place bindings should be mutated. */
data class GamepadBindings(val keyCodes: Map<GamepadAction, Int?> = defaultGamepadBindings())

/** Deliberately excludes KEYCODE_BUTTON_A/B, reserved for menu Confirm/Back. Some controllers
 *  report L2/R2 only as analog axes rather than KeyEvents, which would leave RotateZ+/- dead by
 *  default on that hardware — the user can still rebind them manually. */
fun defaultGamepadBindings(): Map<GamepadAction, Int?> = mapOf(
    GamepadAction.MoveLeft to KeyEvent.KEYCODE_DPAD_LEFT,
    GamepadAction.MoveRight to KeyEvent.KEYCODE_DPAD_RIGHT,
    GamepadAction.MoveForward to KeyEvent.KEYCODE_DPAD_UP,
    GamepadAction.MoveBackward to KeyEvent.KEYCODE_DPAD_DOWN,
    GamepadAction.RotateXPositive to KeyEvent.KEYCODE_BUTTON_X,
    GamepadAction.RotateXNegative to KeyEvent.KEYCODE_BUTTON_Y,
    GamepadAction.RotateYPositive to KeyEvent.KEYCODE_BUTTON_L1,
    GamepadAction.RotateYNegative to KeyEvent.KEYCODE_BUTTON_R1,
    GamepadAction.RotateZPositive to KeyEvent.KEYCODE_BUTTON_L2,
    GamepadAction.RotateZNegative to KeyEvent.KEYCODE_BUTTON_R2,
    GamepadAction.HardDrop to KeyEvent.KEYCODE_BUTTON_THUMBL,
    GamepadAction.Pause to KeyEvent.KEYCODE_BUTTON_START,
)

/** One intPreferencesKey per [GamepadAction] (mirroring [SettingsStore]'s one-key-per-field
 *  convention — every action enumerated individually, no generic/reflective serialization),
 *  generated off the enum since all 12 share the same "action -> nullable keycode" shape. */
private object GamepadKeys {
    val forAction: Map<GamepadAction, androidx.datastore.preferences.core.Preferences.Key<Int>> =
        GamepadAction.entries.associateWith { intPreferencesKey("binding_${it.name}") }
}

/** DataStore-backed persistence for [GamepadBindings]. A stored value of [UNBOUND] means
 *  "explicitly unbound" (distinct from "never saved, use the default"). */
class GamepadBindingsStore(private val context: Context) {

    val bindings: Flow<GamepadBindings> = context.gamepadBindingsDataStore.data.map { prefs ->
        val defaults = defaultGamepadBindings()
        GamepadBindings(
            keyCodes = GamepadAction.entries.associateWith { action ->
                when (val stored = prefs[GamepadKeys.forAction.getValue(action)]) {
                    null -> defaults[action]
                    UNBOUND -> null
                    else -> stored
                }
            },
        )
    }

    suspend fun save(bindings: GamepadBindings) {
        context.gamepadBindingsDataStore.edit { prefs ->
            for (action in GamepadAction.entries) {
                prefs[GamepadKeys.forAction.getValue(action)] = bindings.keyCodes[action] ?: UNBOUND
            }
        }
    }

    suspend fun resetToDefaults() = save(GamepadBindings())

    private companion object {
        const val UNBOUND = -1
    }
}
