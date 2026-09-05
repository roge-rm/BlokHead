package com.rm.blokhead.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMathTest {

    @Test
    fun `exact multiple of step size crosses that many steps with zero remainder`() {
        val (steps, remainder) = stepsFromAccumulated(64f, 32f)
        assertEquals(2, steps)
        assertEquals(0f, remainder, 0.001f)
    }

    @Test
    fun `sub-threshold accumulation crosses no steps and keeps the full remainder`() {
        val (steps, remainder) = stepsFromAccumulated(10f, 32f)
        assertEquals(0, steps)
        assertEquals(10f, remainder, 0.001f)
    }

    @Test
    fun `negative accumulation crosses negative steps`() {
        val (steps, remainder) = stepsFromAccumulated(-70f, 32f)
        assertEquals(-2, steps)
        assertEquals(-6f, remainder, 0.001f)
    }

    @Test
    fun `a fast flick crossing several steps at once reports all of them`() {
        val (steps, remainder) = stepsFromAccumulated(150f, 32f)
        assertEquals(4, steps)
        assertEquals(22f, remainder, 0.001f)
    }

    @Test
    fun `zero accumulation crosses nothing`() {
        val (steps, remainder) = stepsFromAccumulated(0f, 32f)
        assertEquals(0, steps)
        assertEquals(0f, remainder, 0.001f)
    }
}
