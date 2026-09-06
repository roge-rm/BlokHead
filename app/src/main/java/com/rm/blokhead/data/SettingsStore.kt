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
    /** When on, direct touch gestures on the grid itself (drag to move, two-finger pan/twist to
     *  rotate, tap to pause, double-tap to hard drop) work alongside the on-screen buttons — see
     *  [com.rm.blokhead.ui.gestureControls]. Off by default so existing installs see no behavior
     *  change. Independent of [onScreenButtonsEnabled]: either, both, or neither can be on. */
    val gestureControlsEnabled: Boolean = false,
    /** When off, the on-screen D-pad/rotate-cluster buttons (and every other Controls setting
     *  below, which is button-scheme-only) are hidden — for players who've turned on
     *  [gestureControlsEnabled] and no longer want the buttons taking up screen space. On by
     *  default so existing installs see no behavior change. */
    val onScreenButtonsEnabled: Boolean = true,
    val startingDifficulty: Int = 1,
    /** Portrait only. 0f = controls sit right below the grid (default); 1f = pushed down near
     *  the bottom edge. See [landscapeButtonHeight] for landscape's equivalent knob. */
    val portraitButtonHeight: Float = 0f,
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
    /** Portrait only. How far the control clusters sit from the screen's physical left/right
     *  edges, in multiples of one button's width on top of a small fixed gap — 0f = just that
     *  fixed gap (the shipped default), 2f = extra room for a wide intrusion. See
     *  [landscapeButtonInset] for landscape's equivalent knob. */
    val portraitButtonInset: Float = 0f,
    /** Landscape only. Same units/range as [portraitButtonInset], but for landscape's clusters
     *  (and the SCORE/LEVEL/CUBES readouts above them) — 1f is the shipped default here instead
     *  of 0f, since landscape's clusters sit right at the screen's physical edges otherwise,
     *  where a typical camera cutout/gesture-nav area is more likely to cover them. */
    val landscapeButtonInset: Float = 1f,
    /** Landscape only. Where the control clusters sit vertically — 0f = top, 1f = bottom, 0.5f =
     *  centered (the shipped default, matching landscape's original fixed behavior before this
     *  setting existed). See [portraitButtonHeight] for portrait's equivalent knob. */
    val landscapeButtonHeight: Float = 0.5f,
)

private object Keys {
    val DIAGONAL = booleanPreferencesKey("diagonal_buttons_enabled")
    val GESTURE_CONTROLS_ENABLED = booleanPreferencesKey("gesture_controls_enabled")
    val ON_SCREEN_BUTTONS_ENABLED = booleanPreferencesKey("on_screen_buttons_enabled")
    val DIFFICULTY = intPreferencesKey("starting_difficulty")
    val PORTRAIT_BUTTON_HEIGHT = floatPreferencesKey("portrait_button_height")
    // Pre-1.2 name for PORTRAIT_BUTTON_HEIGHT, back when it was portrait's only layout knob —
    // read as a fallback so an existing install doesn't just lose the value it had saved.
    val LEGACY_BUTTON_POSITION = floatPreferencesKey("button_vertical_position")
    val SOUND = booleanPreferencesKey("sound_enabled")
    val LEFT_HANDED = booleanPreferencesKey("left_handed_mode")
    val BLOCK_SET = stringPreferencesKey("block_set")
    val WELL_SIZE = intPreferencesKey("well_size")
    val WELL_HEIGHT = intPreferencesKey("well_height")
    val BUTTON_OPACITY = floatPreferencesKey("button_opacity")
    val BUTTON_SCALE = floatPreferencesKey("button_scale")
    val PORTRAIT_BUTTON_INSET = floatPreferencesKey("portrait_button_inset")
    val LANDSCAPE_BUTTON_INSET = floatPreferencesKey("landscape_button_inset")
    val LANDSCAPE_BUTTON_HEIGHT = floatPreferencesKey("landscape_button_height")
}

/** DataStore-backed persistence for [Settings]. */
class SettingsStore(private val context: Context) {

    val settings: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            diagonalButtonsEnabled = prefs[Keys.DIAGONAL] ?: defaults.diagonalButtonsEnabled,
            gestureControlsEnabled = prefs[Keys.GESTURE_CONTROLS_ENABLED] ?: defaults.gestureControlsEnabled,
            onScreenButtonsEnabled = prefs[Keys.ON_SCREEN_BUTTONS_ENABLED] ?: defaults.onScreenButtonsEnabled,
            startingDifficulty = prefs[Keys.DIFFICULTY] ?: defaults.startingDifficulty,
            portraitButtonHeight = prefs[Keys.PORTRAIT_BUTTON_HEIGHT]
                ?: prefs[Keys.LEGACY_BUTTON_POSITION]
                ?: defaults.portraitButtonHeight,
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
            landscapeButtonHeight = prefs[Keys.LANDSCAPE_BUTTON_HEIGHT] ?: defaults.landscapeButtonHeight,
        )
    }

    suspend fun save(settings: Settings) {
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
