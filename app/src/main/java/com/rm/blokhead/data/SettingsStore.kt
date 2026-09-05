package com.rm.blokhead.data

import android.content.Context
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

/** User-adjustable gameplay/control preferences — new; the original had no in-game settings
 *  menu, just a couple of hardcoded globals (solidBlocks, backgroundColor) set at compile time. */
data class Settings(
    val diagonalButtonsEnabled: Boolean = false,
    val startingDifficulty: Int = 1,
    /** 0f = controls sit right below the grid (default); 1f = pushed down near the bottom edge. */
    val buttonVerticalPosition: Float = 0f,
    val soundEnabled: Boolean = true,
    val leftHandedMode: Boolean = false,
    val blockSet: BlockSet = BlockSet.ALL,
    /** Applied to both width and depth — the well stays square, since the renderer's camera and
     *  wall-grid math (BlokoutRenderer.setUpCamera, Geometry.perceptuallyEvenZRings) assume it. */
    val wellSize: Int = 5,
    /** The well's vertical extent — how many layers tall it is before game over. */
    val wellHeight: Int = 20,
    /** Opacity of the on-screen move/rotate/drop buttons (1f = fully opaque). */
    val buttonOpacity: Float = 1f,
    /** Size of the on-screen move/rotate/drop buttons, relative to their default (1f = 100%). */
    val buttonScale: Float = 1f,
)

private object Keys {
    val DIAGONAL = booleanPreferencesKey("diagonal_buttons_enabled")
    val DIFFICULTY = intPreferencesKey("starting_difficulty")
    val BUTTON_POSITION = floatPreferencesKey("button_vertical_position")
    val SOUND = booleanPreferencesKey("sound_enabled")
    val LEFT_HANDED = booleanPreferencesKey("left_handed_mode")
    val BLOCK_SET = stringPreferencesKey("block_set")
    val WELL_SIZE = intPreferencesKey("well_size")
    val WELL_HEIGHT = intPreferencesKey("well_height")
    val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
    val BUTTON_SCALE = floatPreferencesKey("button_scale")
}

/** DataStore-backed persistence for [Settings]. */
class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            diagonalButtonsEnabled = prefs[Keys.DIAGONAL] ?: defaults.diagonalButtonsEnabled,
            startingDifficulty = prefs[Keys.DIFFICULTY] ?: defaults.startingDifficulty,
            buttonVerticalPosition = prefs[Keys.BUTTON_POSITION] ?: defaults.buttonVerticalPosition,
            soundEnabled = prefs[Keys.SOUND] ?: defaults.soundEnabled,
            leftHandedMode = prefs[Keys.LEFT_HANDED] ?: defaults.leftHandedMode,
            blockSet = prefs[Keys.BLOCK_SET]?.let { name -> runCatching { BlockSet.valueOf(name) }.getOrNull() }
                ?: defaults.blockSet,
            wellSize = prefs[Keys.WELL_SIZE] ?: defaults.wellSize,
            wellHeight = prefs[Keys.WELL_HEIGHT] ?: defaults.wellHeight,
            buttonOpacity = prefs[Keys.BUTTON_OPACITY] ?: defaults.buttonOpacity,
            buttonScale = prefs[Keys.BUTTON_SCALE] ?: defaults.buttonScale,
        )
    }

    suspend fun save(settings: Settings) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DIAGONAL] = settings.diagonalButtonsEnabled
            prefs[Keys.DIFFICULTY] = settings.startingDifficulty
            prefs[Keys.BUTTON_POSITION] = settings.buttonVerticalPosition
            prefs[Keys.SOUND] = settings.soundEnabled
            prefs[Keys.LEFT_HANDED] = settings.leftHandedMode
            prefs[Keys.BLOCK_SET] = settings.blockSet.name
            prefs[Keys.WELL_SIZE] = settings.wellSize
            prefs[Keys.WELL_HEIGHT] = settings.wellHeight
            prefs[Keys.BUTTON_OPACITY] = settings.buttonOpacity
            prefs[Keys.BUTTON_SCALE] = settings.buttonScale
        }
    }
}
