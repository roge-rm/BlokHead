package com.rm.blokhead.render

import androidx.compose.ui.graphics.Color

// Colors shared by both renderers (Android's GLES BlokoutRenderer and the browser's Canvas one),
// so the game looks the same on each. Colors are rgba floats in 0..1.

/** The GL surface's clear color, exposed as a Compose [Color] so the landscape layout can paint
 *  the pillarbox margins around the well the exact same shade — otherwise they'd default to
 *  [androidx.compose.material3.ColorScheme.background], which is light in light theme and
 *  wouldn't read as the intended dark margin regardless of system theme. */
val wellBackgroundColor: Color = Color(red = 0.03f, green = 0.04f, blue = 0.045f)

val WELL_LINE_COLOR = floatArrayOf(0.85f, 0.85f, 0.1f, 1f)

// The falling piece is drawn as a wireframe only (no filled faces) so the grid and any locked
// cubes beneath it stay fully visible through it, matching BlockOut II's reference look. A
// faint shaded fill is layered under the wireframe so the piece still reads as a solid form,
// not just outlines, while staying mostly see-through.
val BLOCK_WIRE_COLOR = floatArrayOf(1f, 1f, 1f, 1f)
val BLOCK_FILL_COLOR = floatArrayOf(0.75f, 0.85f, 1f, 0.22f)

/** Deterministic per-layer hue, standing in for the original's randomized layerMaterials. */
fun colorForLayer(z: Int): FloatArray {
    val hue = (z * 47) % 360
    return hsvToRgba(hue.toFloat(), 0.55f, 0.85f)
}

/** A completed layer flashes bright white the instant it completes, then eases back toward
 *  its normal color over [progress] (0f..1f) right up until it's actually removed — a quick
 *  "flash, then reveal-and-vanish" rather than a flat highlight for the whole duration. */
fun flashColor(base: FloatArray, progress: Float): FloatArray = floatArrayOf(
    lerp(1f, base[0], progress),
    lerp(1f, base[1], progress),
    lerp(1f, base[2], progress),
    1f,
)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun hsvToRgba(h: Float, s: Float, v: Float): FloatArray {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return floatArrayOf(r + m, g + m, b + m, 1f)
}
