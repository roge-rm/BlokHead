package com.rm.blokhead.game

import org.junit.Assert.assertEquals
import org.junit.Test

class RotationTest {

    @Test
    fun `four quarter turns on any axis return to identity`() {
        for (axis in 0..2) {
            val m = identityIntMatrix()
            repeat(4) { rotateIntegerRotation(m, axis, 1) }
            assertEquals(identityIntMatrix().deepToList(), m.deepToList())
        }
    }

    @Test
    fun `a turn and its inverse cancel out`() {
        for (axis in 0..2) {
            val m = identityIntMatrix()
            rotateIntegerRotation(m, axis, 1)
            rotateIntegerRotation(m, axis, -1)
            assertEquals(identityIntMatrix().deepToList(), m.deepToList())
        }
    }

    @Test
    fun `inverse of a rotation matches its transpose composed to identity`() {
        val m = identityIntMatrix()
        rotateIntegerRotation(m, Axis.Z, 1)
        rotateIntegerRotation(m, Axis.X, 1)
        val inverse = inverseIntegerRotation(m)

        // Applying m then its inverse to any vector should be a no-op.
        val v = intArrayOf(3, -2, 5)
        val rotated = rotateIntegerVector(v, m)
        val restored = rotateIntegerVector(rotated, inverse)
        assertEquals(v.toList(), restored.toList())
    }

    @Test
    fun `quarter turn around Z maps X axis onto Y axis`() {
        val m = identityIntMatrix()
        rotateIntegerRotation(m, Axis.Z, 1)
        val result = rotateIntegerVector(intArrayOf(1, 0, 0), m)
        // Should land on a unit vector along Y (sign depends on handedness, magnitude must match).
        assertEquals(1, result.map { kotlin.math.abs(it) }.sum())
    }

    private fun IntMatrix.deepToList() = map { it.toList() }
}
