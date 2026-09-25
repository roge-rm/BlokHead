package com.rm.blokhead.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gamepadBindingsDataStore by preferencesDataStore(name = "gamepad_bindings")

/** One intPreferencesKey per [GamepadAction] (mirroring [SettingsStore]'s one-key-per-field
 *  convention — every action enumerated individually, no generic/reflective serialization),
 *  generated off the enum since all 12 share the same "action -> nullable keycode" shape. */
private object GamepadKeys {
    val forAction: Map<GamepadAction, androidx.datastore.preferences.core.Preferences.Key<Int>> =
        GamepadAction.entries.associateWith { intPreferencesKey("binding_${it.name}") }
}

/** DataStore-backed persistence for [GamepadBindings]. A stored value of [UNBOUND] means
 *  "explicitly unbound" (distinct from "never saved, use the default"). */
class GamepadBindingsStore(private val context: Context) : GamepadBindingsRepository {

    override val bindings: Flow<GamepadBindings> = context.gamepadBindingsDataStore.data.map { prefs ->
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

    override suspend fun save(bindings: GamepadBindings) {
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
