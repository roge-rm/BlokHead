package com.rm.blokhead.game

/**
 * A piece shape ported from blokout's Form struct (forms.c/forms.h) — a 3D grid of filled/empty
 * cubes plus a center point used as the pivot for rotation. Geometry-only: the original's
 * vertex/polygon lists exist to build the OpenGL mesh, which belongs to the renderer, not here.
 */
class Form(
    val dimensions: IntArray, // [x, y, z]
    val cubes: IntArray, // flattened [z][y][x], 1 = filled
    val centerPoint: IntArray, // [x, y, z] pivot, pre-increment as in getBlockCenter()
) {
    val numCubes: Int = cubes.count { it != 0 }

    fun cubeAt(x: Int, y: Int, z: Int): Int =
        cubes[z * dimensions[0] * dimensions[1] + y * dimensions[0] + x]
}

/** Matches the original's own "Block Set" menu option (FLAT/EXTENDED), which restricted play to
 *  pieces that are a single layer thick (dimensions[2] == 1, i.e. behave like classic 2D Tetris
 *  pieces extruded flat) versus ones that actually extend into the third dimension. */
enum class BlockSet {
    FLAT,
    EXTENDED,
    ALL,
}

/**
 * All 32 blokout piece shapes, ported from data/forms.dat (originally loaded at runtime from a
 * text file; embedded here since there's no equivalent asset-loading need in the Kotlin port).
 * Format per piece: "dimX dimY dimZ", then dimZ layers of dimY rows of dimX cube flags (top to
 * bottom is z=0..dimZ-1, matching the original's read order), then a "cx cy cz" center point —
 * always (0, 0, 0) for every piece in the original data.
 */
object FormCatalog {
    val allForms: List<Form> = parse(FORMS_DATA)
    val flatForms: List<Form> = allForms.filter { it.dimensions[2] == 1 }
    val extendedForms: List<Form> = allForms.filter { it.dimensions[2] > 1 }

    fun formsFor(blockSet: BlockSet): List<Form> = when (blockSet) {
        BlockSet.FLAT -> flatForms
        BlockSet.EXTENDED -> extendedForms
        BlockSet.ALL -> allForms
    }

    private fun parse(data: String): List<Form> {
        val tokens = ArrayDeque(
            data.lineSequence()
                .map { line -> line.substringBefore('#') }
                .flatMap { it.trim().split(Regex("\\s+")) }
                .filter { it.isNotEmpty() }
                .toList()
        )
        fun nextInt() = tokens.removeFirst().toInt()

        val numForms = nextInt()
        return (0 until numForms).map {
            val dim = intArrayOf(nextInt(), nextInt(), nextInt())
            val cubes = IntArray(dim[0] * dim[1] * dim[2])
            for (z in 0 until dim[2]) for (y in 0 until dim[1]) for (x in 0 until dim[0]) {
                cubes[z * dim[0] * dim[1] + y * dim[0] + x] = if (nextInt() != 0) 1 else 0
            }
            val centerPoint = intArrayOf(nextInt(), nextInt(), nextInt())
            Form(dim, cubes, centerPoint)
        }
    }

    @Suppress("SpellCheckingInspection")
    private const val FORMS_DATA = """
32

# the only single-cube piece
# cube
1 1 1
1
0 0 0

# the only two-cube piece
# small-I
2 1 1
1 1
0 0 0

# three-cube pieces (2)
# big-I
3 1 1
1 1 1
1 0 0
# corner piece
2 2 1
1 1
1 0
0 0 0

# four-cube pieces (7)
# L piece
3 2 1
1 1 1
1 0 0
1 0 0
# small-T piece
3 2 1
1 1 1
0 1 0
1 0 0
# square
2 2 1
1 1
1 1
0 0 0
# S piece
3 2 1
1 1 0
0 1 1
1 0 0
# triangle-corner piece
2 2 2
1 1
1 0
1 0
0 0
0 0 0
# left-upper triangle
2 2 2
1 1
1 0
0 0
1 0
0 0 0
# right-upper triangle
2 2 2
1 1
1 0
0 1
0 0
0 0 0

# five-cube pieces (21)

# flat
# U piece
3 2 1
1 1 1
1 0 1
1 0 0
# car piece
3 2 1
1 1 1
1 1 0
1 0 0
# cross piece
3 3 1
0 1 0
1 1 1
0 1 0
1 1 0
# T piece
3 3 1
1 1 1
0 1 0
0 1 0
1 1 0
# big corner piece
3 3 1
1 1 1
1 0 0
1 0 0
1 1 0
# zigzag piece
3 3 1
0 1 1
1 1 0
1 0 0
1 1 0

# extend into three directions
# snout piece
3 2 2
1 1 1
0 0 0
0 1 0
0 1 0
1 0 0
# ridge piece
3 2 2
0 1 1
1 1 0
0 1 0
0 0 0
1 0 0
# inverted ridge piece
3 2 2
1 1 0
0 1 1
0 1 0
0 0 0
1 0 0
# snout-ridge piece
3 2 2
1 1 0
0 1 1
1 0 0
0 0 0
1 0 0
# inverted snout-ridge piece
3 2 2
0 1 1
1 1 0
0 0 1
0 0 0
1 0 0
# edge-corner piece
3 2 2
1 1 1
0 1 0
0 1 0
0 0 0
1 0 0
# long corner piece
3 2 2
1 1 1
1 0 0
1 0 0
0 0 0
1 0 0
# gawky piece
3 2 2
1 1 1
1 0 0
0 0 0
1 0 0
1 0 0
# inverted gawky piece
3 2 2
1 1 1
0 0 1
0 0 0
0 0 1
1 0 0
# lt piece
3 2 2
1 1 1
1 0 0
0 1 0
0 0 0
1 0 0
# inverted lt piece
3 2 2
1 1 1
0 0 1
0 1 0
0 0 0
1 0 0
# pupa piece
3 2 2
1 1 1
1 0 0
0 0 1
0 0 0
1 0 0
# inverted pupa piece
3 2 2
1 1 1
0 0 1
1 0 0
0 0 0
1 0 0
# chunky piece
2 2 2
1 1
1 1
1 0
0 0
0 0 0
# tv piece
2 2 2
1 1
1 0
0 1
1 0
0 0 0
"""
}
