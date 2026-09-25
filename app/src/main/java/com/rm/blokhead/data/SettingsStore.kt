package com.rm.blokhead.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rm.blokhead.game.BlockSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

private object Keys {
    val DIAGONAL = booleanPreferencesKey("diagonal_buttons_enabled")
    val GESTURE_CONTROLS_ENABLED = booleanPreferencesKey("gesture_controls_enabled")
    val ON_SCREEN_BUTTONS_ENABLED = booleanPreferencesKey("on_screen_buttons_enabled")
    val DIFFICULTY = intPreferencesKey("starting_difficulty")
    // The "_v2" keys hold Button Height under the 1.2.6 meaning of the knob (0f = bottom edge,
    // raising it walks the cluster up). Both directions were the other way round before that, so
    // they are new keys rather than reinterpreted old ones — a downgrade then finds its own key
    // untouched instead of a value that means the opposite of what it expects. The pre-1.2.6
    // portrait keys ("portrait_button_height", and "button_vertical_position" before that) are
    // deliberately not read at all; see [Preferences.portraitButtonHeight] for why.
    val PORTRAIT_BUTTON_HEIGHT = floatPreferencesKey("portrait_button_height_v2")
    val LANDSCAPE_BUTTON_HEIGHT = floatPreferencesKey("landscape_button_height_v2")
    val LEGACY_LANDSCAPE_BUTTON_HEIGHT = floatPreferencesKey("landscape_button_height")
    val SOUND = booleanPreferencesKey("sound_enabled")
    val LEFT_HANDED = booleanPreferencesKey("left_handed_mode")
    val BLOCK_SET = stringPreferencesKey("block_set")
    val WELL_SIZE = intPreferencesKey("well_size")
    val WELL_HEIGHT = intPreferencesKey("well_height")
    val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
    val BUTTON_SCALE = floatPreferencesKey("button_scale")
    val PORTRAIT_BUTTON_INSET = floatPreferencesKey("portrait_button_inset")
    val LANDSCAPE_BUTTON_INSET = floatPreferencesKey("landscape_button_inset")
}

/** Reads portrait's Button Height, carrying a pre-1.2.6 value across the change in what the knob
 *  means. Old portrait values are deliberately *dropped* rather than converted: that range ran
 *  from the bottom of the rendered grid to the bottom of the screen, which on a phone was about
 *  30dp of travel and on a 4:3 tablet was no travel at all (both ends landed past the bottom
 *  edge). Every value it could hold therefore described "as low as the buttons go", which is what
 *  the new default of 0f already is — so re-reading an old value as 1f - it, the way
 *  [migratedLandscapeButtonHeight] legitimately can, would move buttons that were sitting at the
 *  bottom of the screen up into the middle of the well. */
private fun Preferences.portraitButtonHeight(default: Float): Float = this[Keys.PORTRAIT_BUTTON_HEIGHT] ?: default

/** Reads landscape's Button Height, converting a pre-1.2.6 value. Landscape's old range genuinely
 *  spanned the screen (0f = top, 1f = bottom) and the new one still does, only counting from the
 *  other end — so the old value maps exactly onto the new one by 1f - it, and a player who had
 *  moved these keeps the position they chose. */
private fun Preferences.landscapeButtonHeight(default: Float): Float =
    this[Keys.LANDSCAPE_BUTTON_HEIGHT]
        ?: this[Keys.LEGACY_LANDSCAPE_BUTTON_HEIGHT]?.let { 1f - it }
        ?: default

/** DataStore-backed persistence for [Settings]. */
class SettingsStore(private val context: Context) : SettingsRepository {

    override val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            diagonalButtonsEnabled = prefs[Keys.DIAGONAL] ?: defaults.diagonalButtonsEnabled,
            gestureControlsEnabled = prefs[Keys.GESTURE_CONTROLS_ENABLED] ?: defaults.gestureControlsEnabled,
            onScreenButtonsEnabled = prefs[Keys.ON_SCREEN_BUTTONS_ENABLED] ?: defaults.onScreenButtonsEnabled,
            startingDifficulty = prefs[Keys.DIFFICULTY] ?: defaults.startingDifficulty,
            portraitButtonHeight = prefs.portraitButtonHeight(defaults.portraitButtonHeight),
            soundEnabled = prefs[Keys.SOUND] ?: defaults.soundEnabled,
            leftHandedMode = prefs[Keys.LEFT_HANDED] ?: defaults.leftHandedMode,
            blockSet = prefs[Keys.BLOCK_SET]?.let { name -> runCatching { BlockSet.valueOf(name) }.getOrNull() }
                ?: defaults.blockSet,
            wellSize = prefs[Keys.WELL_SIZE] ?: defaults.wellSize,
            wellHeight = prefs[Keys.WELL_HEIGHT] ?: defaults.wellHeight,
            buttonOpacity = prefs[Keys.BUTTON_OPACITY] ?: defaults.buttonOpacity,
            buttonScale = prefs[Keys.BUTTON_SCALE] ?: defaults.buttonScale,
            portraitButtonInset = prefs[Keys.PORTRAIT_BUTTON_INSET] ?: defaults.portraitButtonInset,
            landscapeButtonInset = prefs[Keys.LANDSCAPE_BUTTON_INSET] ?: defaults.landscapeButtonInset,
            landscapeButtonHeight = prefs.landscapeButtonHeight(defaults.landscapeButtonHeight),
        )
    }

    override suspend fun save(settings: Settings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DIAGONAL] = settings.diagonalButtonsEnabled
            prefs[Keys.GESTURE_CONTROLS_ENABLED] = settings.gestureControlsEnabled
            prefs[Keys.ON_SCREEN_BUTTONS_ENABLED] = settings.onScreenButtonsEnabled
            prefs[Keys.DIFFICULTY] = settings.startingDifficulty
            prefs[Keys.PORTRAIT_BUTTON_HEIGHT] = settings.portraitButtonHeight
            prefs[Keys.SOUND] = settings.soundEnabled
            prefs[Keys.LEFT_HANDED] = settings.leftHandedMode
            prefs[Keys.BLOCK_SET] = settings.blockSet.name
            prefs[Keys.WELL_SIZE] = settings.wellSize
            prefs[Keys.WELL_HEIGHT] = settings.wellHeight
            prefs[Keys.BUTTON_OPACITY] = settings.buttonOpacity
            prefs[Keys.BUTTON_SCALE] = settings.buttonScale
            prefs[Keys.PORTRAIT_BUTTON_INSET] = settings.portraitButtonInset
            prefs[Keys.LANDSCAPE_BUTTON_INSET] = settings.landscapeButtonInset
            prefs[Keys.LANDSCAPE_BUTTON_HEIGHT] = settings.landscapeButtonHeight
        }
    }
}
