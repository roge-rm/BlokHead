package com.rm.blokhead.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.rm.blokhead.ui.theme.WellLineYellow
import kotlin.math.min

/** One cube in [WellPreview], in well cells. [z] is up from the floor. */
data class PreviewCube(
    val x: Int,
    val y: Int,
    val z: Int,
    val top: Color,
    val left: Color,
    val right: Color,
)

/** How many layers [WellPreview] actually draws. The play well is 10..24 layers; map that onto
 *  6..12 so the settings height slider moves the picture without a 24-layer pit overflowing. */
internal fun previewDrawHeight(wellHeight: Int): Int {
    val clamped = wellHeight.coerceIn(10, 24)
    return 6 + (clamped - 10) * 6 / 14
}

/** Horizontal wall rings from the floor to the rim, even steps so the last gap matches the rest. */
internal fun previewZRings(height: Int): List<Float> {
    if (height <= 0) return listOf(0f)
    val step = (2..5).firstOrNull { height % it == 0 } ?: 1
    return (0..height step step).map { it.toFloat() }
}

/**
 * Isometric open-top well: floor grid, wall bars, and even horizontal rings up the height.
 * Cubes are three faces with a dark stroke, far-to-near by [PreviewCube.x]+[PreviewCube.y].
 */
@Composable
fun WellPreview(
    wellSize: Int,
    wellHeight: Int,
    modifier: Modifier = Modifier,
    cubes: List<PreviewCube> = emptyList(),
    lineAlpha: Float = 1f,
) {
    val n = wellSize.coerceAtLeast(1)
    val h = previewDrawHeight(wellHeight)
    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val pad = min(size.width, size.height) * 0.06f
        val availW = (size.width - 2f * pad).coerceAtLeast(1f)
        val availH = (size.height - 2f * pad).coerceAtLeast(1f)
        val tileW = min(availW / n, availH * 2f / (n + h))
        val tileH = tileW / 2f
        val originX = size.width / 2f
        val originY = size.height / 2f - (n - h) * tileH / 2f
        val lineColor = WellLineYellow.copy(alpha = lineAlpha)
        val stroke = 1.2.dp.toPx()

        fun iso(x: Float, y: Float, z: Float): Offset = Offset(
            originX + (x - y) * tileW / 2f,
            originY + (x + y) * tileH / 2f - z * tileH,
        )

        fun wellLine(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
            drawLine(lineColor, iso(x0, y0, z0), iso(x1, y1, z1), stroke, cap = StrokeCap.Round)
        }

        val nf = n.toFloat()
        val hf = h.toFloat()
        val rings = previewZRings(h)
        // Floor (z = 0).
        for (x in 0..n) wellLine(x.toFloat(), 0f, 0f, x.toFloat(), nf, 0f)
        for (y in 0..n) wellLine(0f, y.toFloat(), 0f, nf, y.toFloat(), 0f)
        // All four walls: a bar at every cell, plus even rings up the height (including the rim).
        for (y in intArrayOf(0, n)) {
            val yf = y.toFloat()
            for (x in 0..n) wellLine(x.toFloat(), yf, 0f, x.toFloat(), yf, hf)
            for (z in rings) {
                if (z == 0f) continue
                wellLine(0f, yf, z, nf, yf, z)
            }
        }
        for (x in intArrayOf(0, n)) {
            val xf = x.toFloat()
            for (y in 1 until n) wellLine(xf, y.toFloat(), 0f, xf, y.toFloat(), hf)
            for (z in rings) {
                if (z == 0f) continue
                wellLine(xf, 0f, z, xf, nf, z)
            }
        }

        val cubeStroke = 1.4.dp.toPx()
        cubes.sortedWith(compareBy<PreviewCube> { it.x + it.y }.thenBy { it.z }).forEach { cube ->
            drawPreviewCube(cube, cubeStroke, ::iso)
        }
    }
}

private fun DrawScope.drawPreviewCube(
    cube: PreviewCube,
    strokeWidth: Float,
    iso: (Float, Float, Float) -> Offset,
) {
    val x = cube.x.toFloat()
    val y = cube.y.toFloat()
    val z = cube.z.toFloat()
    val top = arrayOf(iso(x, y, z + 1f), iso(x + 1f, y, z + 1f), iso(x + 1f, y + 1f, z + 1f), iso(x, y + 1f, z + 1f))
    val left = arrayOf(iso(x, y + 1f, z), iso(x + 1f, y + 1f, z), iso(x + 1f, y + 1f, z + 1f), iso(x, y + 1f, z + 1f))
    val right = arrayOf(iso(x + 1f, y, z), iso(x + 1f, y + 1f, z), iso(x + 1f, y + 1f, z + 1f), iso(x + 1f, y, z + 1f))
    val stroke = Color(0xFF0B1220)
    drawFace(left, cube.left, stroke, strokeWidth)
    drawFace(right, cube.right, stroke, strokeWidth)
    drawFace(top, cube.top, stroke, strokeWidth)
}

private fun DrawScope.drawFace(corners: Array<Offset>, fill: Color, stroke: Color, strokeWidth: Float) {
    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
