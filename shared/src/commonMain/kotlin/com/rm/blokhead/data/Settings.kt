package com.rm.blokhead.data

import com.rm.blokhead.game.BlockSet

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
    /** Portrait (and square) layouts. Where the control clusters sit vertically: 0f = resting on
     *  the bottom edge, clear of the navigation bar (the shipped default); 1f = raised until they
     *  straddle the halfway line, i.e. vertically centered. The range is measured off the window
     *  itself rather than off the rendered grid, so it means the same thing on every screen — see
     *  MainActivity.kt's `clusterTopFor`. See [landscapeButtonHeight] for landscape's own copy of
     *  the same knob. */
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
    /** Landscape only. Same direction as [portraitButtonHeight] — 0f = resting on the bottom edge
     *  — but the travel runs the whole height of the screen rather than stopping halfway, so 1f is
     *  the top edge and 0.5f is vertically centered. Landscape's clusters sit in the pillarbox
     *  margins beside the grid rather than over it, so a raised cluster covers no play area and
     *  there is nothing for the range to stop short of. 0.5f is the shipped default because
     *  centered is what these have defaulted to since before this setting existed. */
    val landscapeButtonHeight: Float = 0.5f,
)
