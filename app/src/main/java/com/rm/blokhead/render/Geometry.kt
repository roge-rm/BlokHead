package com.rm.blokhead.render

import kotlin.math.max

private const val FLOATS_PER_VERTEX = 7 // x, y, z, r, g, b, a

/** Fixed light direction matching the original's GL_LIGHT0 position (-1, 1, 1). */
private val LIGHT_DIR = normalize(floatArrayOf(-1f, 1f, 1f))

private fun normalize(v: FloatArray): FloatArray {
    val len = kotlin.math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
    return floatArrayOf(v[0] / len, v[1] / len, v[2] / len)
}

private data class Face(val normal: FloatArray, val corners: Array<FloatArray>)

// A unit cube from (0,0,0) to (1,1,1), as 6 faces of 4 corners each (CCW when viewed from outside).
private val UNIT_CUBE_FACES = listOf(
    Face(floatArrayOf(0f, 0f, -1f), arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 1f, 0f), floatArrayOf(1f, 1f, 0f), floatArrayOf(1f, 0f, 0f))),
    Face(floatArrayOf(0f, 0f, 1f), arrayOf(floatArrayOf(0f, 0f, 1f), floatArrayOf(1f, 0f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(0f, 1f, 1f))),
    Face(floatArrayOf(0f, -1f, 0f), arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 0f, 0f), floatArrayOf(1f, 0f, 1f), floatArrayOf(0f, 0f, 1f))),
    Face(floatArrayOf(0f, 1f, 0f), arrayOf(floatArrayOf(0f, 1f, 0f), floatArrayOf(0f, 1f, 1f), floatArrayOf(1f, 1f, 1f), floatArrayOf(1f, 1f, 0f))),
    Face(floatArrayOf(-1f, 0f, 0f), arrayOf(floatArrayOf(0f, 0f, 0f), floatArrayOf(0f, 0f, 1f), floatArrayOf(0f, 1f, 1f), floatArrayOf(0f, 1f, 0f))),
    Face(floatArrayOf(1f, 0f, 0f), arrayOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(1f, 1f, 0f), floatArrayOf(1f, 1f, 1f), floatArrayOf(1f, 0f, 1f))),
)

// The 12 edges of the unit cube, as (corner, corner) pairs.
private val UNIT_CUBE_EDGES: List<Pair<FloatArray, FloatArray>> = run {
    val c = { x: Int, y: Int, z: Int -> floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat()) }
    val corners = (0..1).flatMap { x -> (0..1).flatMap { y -> (0..1).map { z -> Triple(x, y, z) } } }
    corners.flatMap { (x, y, z) ->
        buildList {
            if (x == 0) add(c(0, y, z) to c(1, y, z))
            if (y == 0) add(c(x, 0, z) to c(x, 1, z))
            if (z == 0) add(c(x, y, 0) to c(x, y, 1))
        }
    }
}

/** CPU-side mesh building. Every cube is baked with simple per-face directional shading (a stand-
 *  in for the original's GL_LIGHT0 + flat shading) directly into vertex colors, since the GLES2
 *  shader here has no per-fragment lighting. Builds are cheap (a handful of cubes at a time), so
 *  meshes are simply rebuilt each frame rather than cached/updated incrementally. */
object Geometry {

    /** Appends one axis-aligned unit cube at grid-space [origin] (its (0,0,0) corner), tinted by
     *  [color] (rgba, 0..1) and shaded per-face by [LIGHT_DIR], into [out]. */
    fun appendCube(out: MutableList<Float>, origin: FloatArray, color: FloatArray) {
        for (face in UNIT_CUBE_FACES) {
            val shade = max(0.35f, 0.4f + 0.6f * dot(face.normal, LIGHT_DIR))
            val r = color[0] * shade
            val g = color[1] * shade
            val b = color[2] * shade
            // Fan the quad into two triangles: (0,1,2) and (0,2,3).
            for (i in intArrayOf(0, 1, 2, 0, 2, 3)) {
                val c = face.corners[i]
                out.add(origin[0] + c[0]); out.add(origin[1] + c[1]); out.add(origin[2] + c[2])
                out.add(r); out.add(g); out.add(b); out.add(color[3])
            }
        }
    }

    private fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    /** Appends the 12 edges of one axis-aligned unit cube at grid-space [origin], as GL_LINES
     *  vertex data tinted a flat [color] (no shading — an outline, not a shaded solid). Used for
     *  the falling piece so the grid/stack beneath it stays fully visible through it. */
    fun appendCubeWireframe(out: MutableList<Float>, origin: FloatArray, color: FloatArray) {
        fun vertex(x: Float, y: Float, z: Float) {
            out.add(origin[0] + x); out.add(origin[1] + y); out.add(origin[2] + z)
            out.add(color[0]); out.add(color[1]); out.add(color[2]); out.add(color[3])
        }
        for ((a, b) in UNIT_CUBE_EDGES) {
            vertex(a[0], a[1], a[2])
            vertex(b[0], b[1], b[2])
        }
    }

    /** Picks wall-ring Z positions that land evenly spaced *on screen* rather than evenly spaced
     *  in world space. A perspective camera's projected scale falls off as ~1/distance, so a
     *  uniform world-space step compresses into a dense, uneven-looking cluster at the far end of
     *  a well much deeper than it is wide (which this one is, looking straight down that depth
     *  axis) — arcade Blockout references with a shallow pit don't hit this because their whole
     *  depth range is small relative to camera distance in the first place. Spacing the rings
     *  evenly in 1/distance (harmonic in world Z) instead reproduces that same even look
     *  regardless of how deep the well actually is. Always includes both z=0 and z=height.
     */
    fun perceptuallyEvenZRings(height: Int, eyeDistanceAboveTop: Float, rungCount: Int): List<Float> {
        if (rungCount <= 1 || height <= 0) return listOf(0f, height.toFloat())
        val eyeZ = height + eyeDistanceAboveTop
        val uFar = 1f / eyeZ // at z = 0, the most distant ring from the camera
        val uNear = 1f / eyeDistanceAboveTop // at z = height, the closest ring to the camera
        return (0..rungCount).map { i ->
            val u = uFar + (uNear - uFar) * i / rungCount
            (eyeZ - 1f / u).coerceIn(0f, height.toFloat())
        }
    }

    /** Builds the well's wireframe grid: a full unit grid on the floor AND all four side walls
     *  (matching the reference's gridded/textured walls, not just a floor grid with plain corner
     *  edges), as GL_LINES vertex data (no color baked in — callers set a uniform color by
     *  drawing this list with a fixed vertex color instead). [zRings] are the wall's horizontal
     *  ring positions (see [perceptuallyEvenZRings]) — the floor's own grid is unaffected, since
     *  every one of its lines lies at the single, fixed depth z=0. */
    fun buildWellGridLines(width: Int, depth: Int, height: Int, zRings: List<Float>, color: FloatArray): FloatList {
        val out = ArrayList<Float>()
        fun line(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float) {
            out.add(x0); out.add(y0); out.add(z0); out.add(color[0]); out.add(color[1]); out.add(color[2]); out.add(color[3])
            out.add(x1); out.add(y1); out.add(z1); out.add(color[0]); out.add(color[1]); out.add(color[2]); out.add(color[3])
        }
        // Floor grid (z = 0).
        for (x in 0..width) line(x.toFloat(), 0f, 0f, x.toFloat(), depth.toFloat(), 0f)
        for (y in 0..depth) line(0f, y.toFloat(), 0f, width.toFloat(), y.toFloat(), 0f)

        // Front/back walls (y = 0 and y = depth): a unit grid over x, ringed over z.
        for (y in intArrayOf(0, depth)) {
            for (x in 0..width) line(x.toFloat(), y.toFloat(), 0f, x.toFloat(), y.toFloat(), height.toFloat())
            for (z in zRings) line(0f, y.toFloat(), z, width.toFloat(), y.toFloat(), z)
        }
        // Left/right walls (x = 0 and x = width): a unit grid over y, ringed over z.
        for (x in intArrayOf(0, width)) {
            for (y in 0..depth) line(x.toFloat(), y.toFloat(), 0f, x.toFloat(), y.toFloat(), height.toFloat())
            for (z in zRings) line(x.toFloat(), 0f, z, x.toFloat(), depth.toFloat(), z)
        }
        return out
    }
}

typealias FloatList = List<Float>

fun FloatList.toFloatArray(): FloatArray = FloatArray(size) { this[it] }

const val FLOATS_PER_CUBE_VERTEX = FLOATS_PER_VERTEX
