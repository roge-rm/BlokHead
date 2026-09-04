package com.rm.blokhead.game

import kotlin.math.abs
import kotlin.math.floor

/**
 * Movement/rotation legality and gravity, ported from blokout's collision.c. All "time" values
 * are seconds elapsed since the current block spawned (see [Block] doc).
 */
object Collision {

    private fun isLegalPosition(block: Block, blockDim: IntArray, blockCenter: IntArray, tube: Tube, pos: IntArray): Boolean {
        for (pz in 0 until blockDim[2]) for (py in 0 until blockDim[1]) for (px in 0 until blockDim[0]) {
            val tx = pos[0] + px - blockCenter[0]
            val ty = pos[1] + py - blockCenter[1]
            val tz = pos[2] + pz + tube.dimensions[2] - blockCenter[2]
            if (tube.isFilled(tx, ty, tz) && block.cubeAt(intArrayOf(px, py, pz))) return false
        }
        return true
    }

    private fun isLegalMove(tube: Tube, block: Block, axis: Int, sign: Int): Boolean {
        val blockDim = block.dimensions()
        val blockCenter = block.center(blockDim)
        if (sign < 0 && block.targetPosition[axis] <= blockCenter[axis]) return false
        if (sign > 0 && block.targetPosition[axis] - blockCenter[axis] + blockDim[axis] >= tube.dimensions[axis]) return false

        val target = intArrayOf(block.targetPosition[0], block.targetPosition[1], floor(block.position[2]).toInt())
        target[axis] += sign
        return isLegalPosition(block, blockDim, blockCenter, tube, target)
    }

    private fun isLegalTurn(tube: Tube, block: Block): Boolean {
        val blockDim = block.dimensions()
        val blockCenter = block.center(blockDim)
        val pos = intArrayOf(block.targetPosition[0], block.targetPosition[1], floor(block.position[2]).toInt())
        return isLegalPosition(block, blockDim, blockCenter, tube, pos)
    }

    fun tryMoveBlock(tube: Tube, block: Block, axis: Int, sign: Int, time: Float) {
        if (isLegalMove(tube, block, axis, sign)) block.moveTo(axis, sign, time)
    }

    fun tryTurnBlock(tube: Tube, block: Block, axis: Int, sign: Int, time: Float) {
        val savedOrientation = block.targetOrientation.copyMatrix()
        rotateIntegerRotation(block.targetOrientation, axis, sign)
        // Dimensions/center depend only on orientation, which is now fixed for the rest of this
        // call, so (as in the original) they're computed once rather than refreshed per move.
        val blockDim = block.dimensions()
        val blockCenter = block.center(blockDim)
        var ok = true
        val moves = intArrayOf(0, 0)

        for (i in 0..1) {
            if (!ok) break
            while (block.targetPosition[i] < blockCenter[i]) {
                if (isLegalMove(tube, block, i, 1)) {
                    block.targetPosition[i]++
                    moves[i]++
                } else {
                    ok = false
                    break
                }
            }
            if (!ok) break
            while (block.targetPosition[i] - blockCenter[i] + blockDim[i] > tube.dimensions[i]) {
                if (isLegalMove(tube, block, i, -1)) {
                    block.targetPosition[i]--
                    moves[i]--
                } else {
                    ok = false
                    break
                }
            }
        }

        if (ok && !isLegalTurn(tube, block)) ok = false
        for (i in 0..2) block.targetOrientation[i] = savedOrientation[i]

        for (i in 0..1) block.targetPosition[i] -= moves[i]
        if (ok) {
            for (i in 0..1) {
                while (moves[i] < 0) {
                    block.moveTo(i, -1, time)
                    moves[i]++
                }
                while (moves[i] > 0) {
                    block.moveTo(i, 1, time)
                    moves[i]--
                }
            }
            block.turn(axis, sign, time)
        }
    }

    /** Advances the falling piece's Z position under gravity. Returns true while still falling.
     *  Ported from tryLowerBlock(). */
    fun tryLowerBlock(tube: Tube, block: Block, time: Float): Boolean {
        val blockDim = block.dimensions()
        val blockCenter = block.center(blockDim)

        var newHeight = block.stopHeight - (time - block.lastStop) * block.fallSpeed

        if (newHeight < -tube.dimensions[2] + blockCenter[2] + tube.height + 0.1f) {
            if (newHeight < -tube.dimensions[2] + blockCenter[2]) {
                newHeight = -tube.dimensions[2] + blockCenter[2] + 0.1f
            }

            val pos = intArrayOf(block.targetPosition[0], block.targetPosition[1], 0)
            var pz = floor(block.position[2]).toInt()
            val floorNewHeight = floor(newHeight).toInt()
            while (pz >= floorNewHeight) {
                pos[2] = pz
                if (!isLegalPosition(block, blockDim, blockCenter, tube, pos)) break
                pz--
            }
            if (newHeight < pz + 1) newHeight = pz + 1.1f
        }

        if (newHeight < block.position[2]) {
            block.lastFall = time
            block.position[2] = newHeight
            return true
        }
        block.stopHeight = block.position[2]
        block.lastStop = time
        return false
    }
}
