package com.rm.blokhead.game

import org.junit.Assert.assertEquals
import org.junit.Test

class TubeTest {

    private val cubeForm = FormCatalog.allForms[0] // 1x1x1

    /** Simulates a hard drop: lets the block fall under gravity until it rests. */
    private fun dropToBottom(tube: Tube, block: Block) {
        block.fallSpeed = tube.dimensions[2].toFloat()
        var t = 0f
        while (Collision.tryLowerBlock(tube, block, t)) {
            t += 0.1f
        }
    }

    @Test
    fun `locking a single cube raises height by one`() {
        val tube = Tube(x = 3, y = 3, height = 6)
        val block = Block(cubeForm)
        dropToBottom(tube, block)
        tube.addBlock(block)
        assertEquals(1, tube.height)
    }

    @Test
    fun `filling every column of the bottom layer clears it`() {
        val tube = Tube(x = 2, y = 2, height = 6)
        // Drop a cube into each of the 4 columns of the bottom layer.
        for (x in 0..1) for (y in 0..1) {
            val block = Block(cubeForm)
            block.targetPosition[0] = x
            block.targetPosition[1] = y
            dropToBottom(tube, block)
            tube.addBlock(block)
        }
        // The layer should have cleared back down to empty.
        assertEquals(0, tube.height)
        assertEquals(1, tube.lastDrop)
    }

    @Test
    fun `a piece spanning two layers keeps its shape on both layers when locked`() {
        // The "triangle-corner piece": z=0 has an L of 3 cubes at (0,0) (1,0) (0,1), plus one
        // cube stacked on top at (0,0,1). Regression test for the piece appearing to flatten
        // into a single layer on locking instead of preserving its full 3D shape.
        val triangleCorner = FormCatalog.allForms[8]
        check(triangleCorner.dimensions.toList() == listOf(2, 2, 2))
        check(triangleCorner.numCubes == 4)

        val tube = Tube(x = 3, y = 3, height = 6)
        val block = Block(triangleCorner)
        dropToBottom(tube, block)
        tube.addBlock(block)

        assertEquals(2, tube.height)
        assertEquals(true, tube.isFilled(0, 0, 0))
        assertEquals(true, tube.isFilled(1, 0, 0))
        assertEquals(true, tube.isFilled(0, 1, 0))
        assertEquals(false, tube.isFilled(1, 1, 0))
        assertEquals(true, tube.isFilled(0, 0, 1))
        assertEquals(false, tube.isFilled(1, 0, 1))
        assertEquals(false, tube.isFilled(0, 1, 1))
        assertEquals(false, tube.isFilled(1, 1, 1))
    }
}
