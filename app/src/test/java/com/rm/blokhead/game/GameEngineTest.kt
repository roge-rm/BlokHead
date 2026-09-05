package com.rm.blokhead.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    /** Only the single-cube piece, so drops are deterministic for locking/scoring tests. */
    private val cubeOnly = listOf(FormCatalog.allForms[0])

    @Test
    fun `hard drop eventually locks the block into the tube`() {
        val engine = GameEngine(forms = cubeOnly, width = 3, depth = 3, height = 10, random = Random(0))
        engine.hardDrop()
        var ticks = 0
        while (engine.cubesDropped == 0 && ticks < 1000) {
            engine.update(0.05f)
            ticks++
        }
        assertEquals(1, engine.cubesDropped)
        assertTrue(engine.tube.height >= 1)
    }

    @Test
    fun `game ends once the tube fills near the top`() {
        // Width 2 with every cube dropped into the same spawn column (no move commands issued):
        // one column stacks up while the other stays empty, so layers never complete/clear and
        // height climbs monotonically towards the game-over threshold.
        val engine = GameEngine(forms = cubeOnly, width = 2, depth = 1, height = 6, random = Random(0))
        var ticks = 0
        while (!engine.isGameOver && ticks < 20_000) {
            engine.hardDrop()
            engine.update(0.05f)
            ticks++
        }
        assertTrue(engine.isGameOver)
        assertTrue(engine.cubesDropped > 0)
    }

    @Test
    fun `moving right then left returns the block to its spawn column`() {
        // Every piece spawns at its form's center point (always (0, 0, 0) in the original data),
        // i.e. flush against the low X/Y wall — so moveLeft from spawn is legitimately illegal
        // (no room to move); move right first, then back left, to round-trip legally.
        val engine = GameEngine(forms = cubeOnly, width = 3, depth = 3, height = 10, random = Random(0))
        val startX = engine.currentBlock.targetPosition[0]
        engine.moveRight()
        engine.update(1f) // let the move animation finish
        engine.moveLeft()
        engine.update(1f)
        assertEquals(startX, engine.currentBlock.targetPosition[0])
    }

    @Test
    fun `rotating a cube is a no-op but does not crash`() {
        val engine = GameEngine(forms = cubeOnly, width = 3, depth = 3, height = 10, random = Random(0))
        engine.rotate(Axis.X, 1)
        engine.update(1f)
        assertFalse(engine.isGameOver)
    }

    @Test
    fun `pausing freezes the block in place and ignores input`() {
        val engine = GameEngine(forms = cubeOnly, width = 3, depth = 3, height = 10, random = Random(0))
        engine.setPaused(true)
        val frozenZ = engine.currentBlock.position[2]
        val frozenX = engine.currentBlock.targetPosition[0]

        engine.update(5f)
        engine.moveRight()
        engine.rotate(Axis.X, 1)
        engine.hardDrop()

        assertEquals(frozenZ, engine.currentBlock.position[2])
        assertEquals(frozenX, engine.currentBlock.targetPosition[0])
        assertEquals(0, engine.cubesDropped)
    }

    @Test
    fun `unpausing lets the game continue`() {
        val engine = GameEngine(forms = cubeOnly, width = 3, depth = 3, height = 10, random = Random(0))
        engine.setPaused(true)
        engine.setPaused(false)
        engine.hardDrop()
        var ticks = 0
        while (engine.cubesDropped == 0 && ticks < 1000) {
            engine.update(0.05f)
            ticks++
        }
        assertEquals(1, engine.cubesDropped)
    }

    private fun dropAndSettle(engine: GameEngine, expectedCubesDropped: Int) {
        engine.hardDrop()
        var ticks = 0
        while (engine.cubesDropped < expectedCubesDropped && ticks < 1000) {
            engine.update(0.05f)
            ticks++
        }
    }

    @Test
    fun `a completed layer flashes for a moment before it actually clears`() {
        // Width 2: fill column 0, then move to column 1 and fill it too, completing the bottom
        // layer on the second lock.
        val engine = GameEngine(forms = cubeOnly, width = 2, depth = 1, height = 6, random = Random(0))
        dropAndSettle(engine, expectedCubesDropped = 1)
        assertTrue(engine.pendingClearLayers.isEmpty()) // no clear yet, column 1 still empty

        engine.moveRight()
        engine.update(1f) // finish the move animation before dropping again
        dropAndSettle(engine, expectedCubesDropped = 2)

        // The layer is complete, but should still be flashing, not yet actually removed.
        assertTrue(engine.pendingClearLayers.isNotEmpty())
        val heightWhileFlashing = engine.tube.height

        // Advance well past the flash duration.
        repeat(20) { engine.update(0.05f) }

        assertTrue(engine.pendingClearLayers.isEmpty())
        assertTrue(engine.tube.height < heightWhileFlashing)
    }

    @Test
    fun `input is ignored while a completed layer is flashing`() {
        val engine = GameEngine(forms = cubeOnly, width = 2, depth = 1, height = 6, random = Random(0))
        dropAndSettle(engine, expectedCubesDropped = 1)
        engine.moveRight()
        engine.update(1f)
        dropAndSettle(engine, expectedCubesDropped = 2)
        assertTrue(engine.pendingClearLayers.isNotEmpty())

        val frozenTargetX = engine.currentBlock.targetPosition[0]
        engine.moveLeft()
        engine.rotate(Axis.X, 1)
        engine.hardDrop()
        assertEquals(frozenTargetX, engine.currentBlock.targetPosition[0])
        assertEquals(2, engine.cubesDropped) // hardDrop() during the flash must not lock anything
    }
}
