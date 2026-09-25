package com.rm.blokhead.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.rm.blokhead.game.GameEngine
import com.rm.blokhead.game.Tube
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Draws the same scene as :app's GLES BlokoutRenderer, but with Compose drawing calls, for
 * platforms with no GL surface to hand (the browser build). Same camera, same geometry
 * ([Geometry]), same colors ([SceneColors]); what GL's depth buffer did is done by drawing order:
 *
 *  1. the well grid, which lines the walls and floor and so is behind everything inside the well;
 *  2. locked cubes, only the faces turned towards the camera, farthest first;
 *  3. the falling piece on top: its faint fill (front faces only), then the white edges of those
 *     faces, standing in for GL hiding the back edges behind the fill.
 *
 * Unlike GL, a locked stack taller than the falling piece never hides it; the piece is mostly
 * transparent and outlined, so that reads fine.
 */
class CanvasRenderer {
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val temp = FloatArray(16)
    private val mvp = FloatArray(16)

    fun DrawScope.drawScene(engine: GameEngine) {
        drawRect(wellBackgroundColor)
        if (size.width <= 0f || size.height <= 0f) return
        setUpCamera(engine.tube, size.width / size.height)

        Mat4.setIdentity(model)
        updateMvp()
        drawWellGrid(engine.tube)
        drawLockedCubes(engine)
        if (engine.pendingClearLayers.isEmpty()) drawFallingBlock(engine)
    }

    /** See BlokoutRenderer.wellTop. */
    private fun wellTop(tube: Tube): Float = tube.dimensions[2].toFloat() + 1f

    /** Mirrors BlokoutRenderer.setUpCamera exactly; see its comments. */
    private fun setUpCamera(tube: Tube, aspect: Float) {
        val width = tube.dimensions[0].toFloat()
        val depth = tube.dimensions[1].toFloat()
        val height = wellTop(tube)
        val standoff = maxOf(width, depth) * 1.5f
        val eyeZ = height + standoff
        val nearHalfWidth = maxOf(width, depth) / 2f
        val horizontalHalfAngle = atan(nearHalfWidth / standoff)
        val fovY = (2.0 * atan(tan(horizontalHalfAngle) / aspect) * 180.0 / PI).toFloat()
        Mat4.perspective(projection, fovY, aspect, 0.5f, eyeZ + 5f)
        val cx = width / 2f
        val cy = depth / 2f
        Mat4.lookAt(view, cx, cy, eyeZ, cx, cy, 0f, 0f, 1f, 0f)
    }

    private fun updateMvp() {
        Mat4.multiply(temp, view, model)
        Mat4.multiply(mvp, projection, temp)
    }

    /** A projected point: screen position plus clip-space w, which grows with distance from the
     *  eye and is what faces are sorted by. */
    private class Projected(val x: Float, val y: Float, val w: Float)

    private fun DrawScope.project(x: Float, y: Float, z: Float): Projected {
        val cx = mvp[0] * x + mvp[4] * y + mvp[8] * z + mvp[12]
        val cy = mvp[1] * x + mvp[5] * y + mvp[9] * z + mvp[13]
        val cw = mvp[3] * x + mvp[7] * y + mvp[11] * z + mvp[15]
        return Projected((cx / cw + 1f) / 2f * size.width, (1f - cy / cw) / 2f * size.height, cw)
    }

    private fun DrawScope.drawLineList(data: List<Float>, strokeWidth: Float) {
        var i = 0
        while (i + 2 * FLOATS_PER_CUBE_VERTEX <= data.size) {
            val a = project(data[i], data[i + 1], data[i + 2])
            val b = project(data[i + 7], data[i + 8], data[i + 9])
            drawLine(rgba(data, i + 3), Offset(a.x, a.y), Offset(b.x, b.y), strokeWidth)
            i += 2 * FLOATS_PER_CUBE_VERTEX
        }
    }

    private fun DrawScope.drawWellGrid(tube: Tube) {
        val height = wellTop(tube).toInt()
        val lines = Geometry.buildWellGridLines(
            tube.dimensions[0], tube.dimensions[1], height, Geometry.evenLayerZRings(height), WELL_LINE_COLOR,
        )
        drawLineList(lines, strokeWidth = lineWidth())
    }

    /** One quad of a cube, projected, with its (flat) color. */
    private class ScreenFace(val corners: Array<Projected>, val color: Color) {
        val depth = corners.sumOf { it.w.toDouble() }.toFloat() / corners.size

        /** Geometry's faces wind counter-clockwise seen from outside; screen y points down, so a
         *  face turned towards the camera comes out clockwise, i.e. with positive area here. */
        val facesCamera: Boolean
            get() {
                var twiceArea = 0f
                for (i in corners.indices) {
                    val a = corners[i]
                    val b = corners[(i + 1) % corners.size]
                    twiceArea += a.x * b.y - b.x * a.y
                }
                return twiceArea > 0f
            }

        fun path(): Path = Path().apply {
            moveTo(corners[0].x, corners[0].y)
            for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
            close()
        }
    }

    /** Turns [Geometry.appendCube] output (6 faces x 2 triangles (0,1,2)+(0,2,3)) back into
     *  quads: each face's corners are its vertices 0, 1, 2 and 5. */
    private fun DrawScope.cubeFaces(data: List<Float>): List<ScreenFace> {
        val faces = ArrayList<ScreenFace>(data.size / FLOATS_PER_FACE)
        var base = 0
        while (base + FLOATS_PER_FACE <= data.size) {
            val corners = arrayOf(0, 1, 2, 5).map { v ->
                val o = base + v * FLOATS_PER_CUBE_VERTEX
                project(data[o], data[o + 1], data[o + 2])
            }.toTypedArray()
            faces += ScreenFace(corners, rgba(data, base + 3))
            base += FLOATS_PER_FACE
        }
        return faces
    }

    private fun DrawScope.drawLockedCubes(engine: GameEngine) {
        val tube = engine.tube
        val flashingLayers = engine.pendingClearLayers
        val flashProgress = engine.clearFlashProgress
        val vertices = ArrayList<Float>()
        for (z in 0 until tube.dimensions[2]) {
            val color = if (z in flashingLayers) flashColor(colorForLayer(z), flashProgress) else colorForLayer(z)
            for (y in 0 until tube.dimensions[1]) for (x in 0 until tube.dimensions[0]) {
                if (tube.isFilled(x, y, z)) {
                    Geometry.appendCube(vertices, floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat()), color)
                }
            }
        }
        cubeFaces(vertices)
            .filter { it.facesCamera }
            .sortedByDescending { it.depth }
            .forEach { drawPath(it.path(), it.color) }
    }

    private fun DrawScope.drawFallingBlock(engine: GameEngine) {
        val block = engine.currentBlock
        val form = block.form
        val fill = ArrayList<Float>()
        for (z in 0 until form.dimensions[2]) for (y in 0 until form.dimensions[1]) for (x in 0 until form.dimensions[0]) {
            if (form.cubeAt(x, y, z) != 0) {
                val local = floatArrayOf(
                    x - form.centerPoint[0] - 0.5f,
                    y - form.centerPoint[1] - 0.5f,
                    z - form.centerPoint[2] - 0.5f,
                )
                Geometry.appendCube(fill, local, BLOCK_FILL_COLOR)
            }
        }

        // Same model matrix as BlokoutRenderer.drawFallingBlock; see its comment on the Z re-base.
        Mat4.setIdentity(model)
        Mat4.translate(
            model,
            block.position[0] + 0.5f,
            block.position[1] + 0.5f,
            block.position[2] + engine.tube.dimensions[2] + 0.5f,
        )
        val o = block.orientation
        val rotation = floatArrayOf(
            o[0][0], o[0][1], o[0][2], 0f,
            o[1][0], o[1][1], o[1][2], 0f,
            o[2][0], o[2][1], o[2][2], 0f,
            0f, 0f, 0f, 1f,
        )
        Mat4.multiply(temp, model, rotation)
        temp.copyInto(model)
        updateMvp()

        val front = cubeFaces(fill).filter { it.facesCamera }.sortedByDescending { it.depth }
        front.forEach { drawPath(it.path(), it.color) }
        val wire = Color(BLOCK_WIRE_COLOR[0], BLOCK_WIRE_COLOR[1], BLOCK_WIRE_COLOR[2], BLOCK_WIRE_COLOR[3])
        for (face in front) {
            val c = face.corners
            for (i in c.indices) {
                val a = c[i]
                val b = c[(i + 1) % c.size]
                drawLine(wire, Offset(a.x, a.y), Offset(b.x, b.y), strokeWidth = lineWidth())
            }
        }
    }

    /** GL draws its lines 2 physical pixels wide whatever the density, which on a typical phone
     *  (about 2.6x) is 0.75dp; matching that in dp keeps lines as fine on a 1x desktop screen. */
    private fun DrawScope.lineWidth(): Float = 0.75.dp.toPx().coerceAtLeast(1f)

    private fun rgba(data: List<Float>, offset: Int) =
        Color(data[offset], data[offset + 1], data[offset + 2], data[offset + 3])

    private companion object {
        const val FLOATS_PER_FACE = 6 * FLOATS_PER_CUBE_VERTEX
    }
}

/** The handful of android.opengl.Matrix operations the renderer uses, same column-major layout
 *  and conventions, so the camera matches the GLES renderer's exactly. */
internal object Mat4 {
    fun setIdentity(m: FloatArray) {
        m.fill(0f)
        m[0] = 1f; m[5] = 1f; m[10] = 1f; m[15] = 1f
    }

    /** result = lhs * rhs. [result] must not be [lhs] or [rhs]. */
    fun multiply(result: FloatArray, lhs: FloatArray, rhs: FloatArray) {
        for (i in 0 until 4) for (j in 0 until 4) {
            var sum = 0f
            for (k in 0 until 4) sum += lhs[i + 4 * k] * rhs[k + 4 * j]
            result[i + 4 * j] = sum
        }
    }

    fun translate(m: FloatArray, x: Float, y: Float, z: Float) {
        for (i in 0 until 4) m[12 + i] += m[i] * x + m[4 + i] * y + m[8 + i] * z
    }

    /** android.opengl.Matrix.perspectiveM. */
    fun perspective(m: FloatArray, fovY: Float, aspect: Float, zNear: Float, zFar: Float) {
        val f = 1f / tan(fovY * (PI / 360.0)).toFloat()
        val rangeReciprocal = 1f / (zNear - zFar)
        m.fill(0f)
        m[0] = f / aspect
        m[5] = f
        m[10] = (zFar + zNear) * rangeReciprocal
        m[11] = -1f
        m[14] = 2f * zFar * zNear * rangeReciprocal
    }

    /** android.opengl.Matrix.setLookAtM. */
    fun lookAt(
        m: FloatArray,
        eyeX: Float, eyeY: Float, eyeZ: Float,
        centerX: Float, centerY: Float, centerZ: Float,
        upX: Float, upY: Float, upZ: Float,
    ) {
        var fx = centerX - eyeX
        var fy = centerY - eyeY
        var fz = centerZ - eyeZ
        val rlf = 1f / sqrt(fx * fx + fy * fy + fz * fz)
        fx *= rlf; fy *= rlf; fz *= rlf
        var sx = fy * upZ - fz * upY
        var sy = fz * upX - fx * upZ
        var sz = fx * upY - fy * upX
        val rls = 1f / sqrt(sx * sx + sy * sy + sz * sz)
        sx *= rls; sy *= rls; sz *= rls
        val ux = sy * fz - sz * fy
        val uy = sz * fx - sx * fz
        val uz = sx * fy - sy * fx
        m[0] = sx; m[1] = ux; m[2] = -fx; m[3] = 0f
        m[4] = sy; m[5] = uy; m[6] = -fy; m[7] = 0f
        m[8] = sz; m[9] = uz; m[10] = -fz; m[11] = 0f
        m[12] = 0f; m[13] = 0f; m[14] = 0f; m[15] = 1f
        translate(m, -eyeX, -eyeY, -eyeZ)
    }
}
