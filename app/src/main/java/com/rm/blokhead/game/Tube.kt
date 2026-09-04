package com.rm.blokhead.game

import kotlin.math.floor

/**
 * The well, ported from blokout's Tube (tube.c/tube.h) — a fixed-size grid that accumulates
 * locked cubes and clears full horizontal layers. [dimensions] mirrors the original's
 * `form->dimensions`, which is `[x, y, height + 1]`: one extra Z-layer beyond the visible
 * `height` is always allocated as headroom, and the same `height + 1` value (not `height`) is
 * what the original compares against for both scoring and the game-over threshold — so it's
 * kept here as-is rather than normalized away.
 */
class Tube(x: Int, y: Int, height: Int) {
    val dimensions = intArrayOf(x, y, height + 1)
    val cubes = IntArray(x * y * (height + 1))

    /** Number of occupied layers from the bottom. */
    var height = 0
        private set

    /** Layers cleared by the most recently locked block. */
    var lastDrop = 0

    private fun index(px: Int, py: Int, pz: Int) = pz * dimensions[0] * dimensions[1] + py * dimensions[0] + px

    // Anything outside the stored grid reads as empty space. The original C code indexes one
    // layer above the top of the array on a freshly spawned block's very first collision check
    // (an out-of-bounds read it silently tolerated); treating out-of-range as "no cube" is the
    // faithful behavior for that case and for any other query that strays outside the grid.
    fun isFilled(px: Int, py: Int, pz: Int): Boolean {
        if (px !in 0 until dimensions[0] || py !in 0 until dimensions[1] || pz !in 0 until dimensions[2]) {
            return false
        }
        return cubes[index(px, py, pz)] != 0
    }

    /** Shifts every layer above [layer] down by one and shrinks [height]. Ported from removeTubeLayer(). */
    fun removeLayer(layer: Int) {
        for (z in layer until height) {
            for (y in 0 until dimensions[1]) {
                for (x in 0 until dimensions[0]) {
                    cubes[index(x, y, z)] = cubes[index(x, y, z + 1)]
                }
            }
        }
        height--
        lastDrop++
    }

    /** Locks [block] into the grid at its current target position/orientation, then clears any
     *  full layers it completed. Ported from addBlockToTube(). */
    fun addBlock(block: Block) {
        val blockDim = block.dimensions()
        val blockCenter = block.center(blockDim)
        val blockZ = floor(block.position[2] + 0.5f).toInt() + dimensions[2]

        for (pz in 0 until blockDim[2]) for (py in 0 until blockDim[1]) for (px in 0 until blockDim[0]) {
            if (block.cubeAt(intArrayOf(px, py, pz))) {
                cubes[index(
                    block.targetPosition[0] + px - blockCenter[0],
                    block.targetPosition[1] + py - blockCenter[1],
                    blockZ + pz - blockCenter[2],
                )] = 1
            }
        }

        // Recompute height: the topmost occupied layer, plus one.
        var top = height + blockDim[2]
        while (top >= 0) {
            var occupied = false
            if (top < dimensions[2]) {
                outer@ for (py in 0 until dimensions[1]) for (px in 0 until dimensions[0]) {
                    if (isFilled(px, py, top)) {
                        occupied = true
                        break@outer
                    }
                }
            }
            if (occupied) break
            top--
        }
        height = top + 1

        // Clear any layers the new block completed.
        var z = blockZ - blockCenter[2]
        val zEnd = z + blockDim[2]
        while (z < zEnd && z < dimensions[2]) {
            var full = true
            outer@ for (py in 0 until dimensions[1]) for (px in 0 until dimensions[0]) {
                if (!isFilled(px, py, z)) {
                    full = false
                    break@outer
                }
            }
            if (full) {
                removeLayer(z)
                z--
            }
            z++
        }
    }
}
