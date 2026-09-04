package com.rm.blokhead.game

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rotation math ported from blokout's transforms.c. Block orientation is tracked two ways:
 * an exact 3x3 signed-permutation [IntMatrix] for grid/collision logic (always axis-aligned,
 * entries in {-1, 0, 1}), and a continuous [FloatMatrix] used only to interpolate the visual
 * orientation smoothly between turns.
 */
typealias IntMatrix = Array<IntArray>
typealias FloatMatrix = Array<FloatArray>

fun identityIntMatrix(): IntMatrix = Array(3) { i -> IntArray(3) { j -> if (i == j) 1 else 0 } }

fun identityFloatMatrix(): FloatMatrix = Array(3) { i -> FloatArray(3) { j -> if (i == j) 1f else 0f } }

fun IntMatrix.copyMatrix(): IntMatrix = Array(3) { i -> this[i].copyOf() }

/** Builds a rotation matrix from Euler angles (radians), one axis-rotation per component. */
fun createRotation(a: FloatArray): FloatMatrix {
    val m = identityFloatMatrix()
    m[0][0] = (cos(a[1]) * cos(a[2])).toFloat()
    m[0][1] = (cos(a[2]) * sin(a[0]) * sin(a[1]) + cos(a[0]) * sin(a[2])).toFloat()
    m[0][2] = (-(cos(a[0]) * cos(a[2]) * sin(a[1])) + sin(a[0]) * sin(a[2])).toFloat()
    m[1][0] = (-(cos(a[1]) * sin(a[2]))).toFloat()
    m[1][1] = (cos(a[0]) * cos(a[2]) - sin(a[0]) * sin(a[1]) * sin(a[2])).toFloat()
    m[1][2] = (cos(a[2]) * sin(a[0]) + cos(a[0]) * sin(a[1]) * sin(a[2])).toFloat()
    m[2][0] = sin(a[1]).toFloat()
    m[2][1] = (-(cos(a[1]) * sin(a[0]))).toFloat()
    m[2][2] = (cos(a[0]) * cos(a[1])).toFloat()
    return m
}

fun rotateVector(v: FloatArray, rot: FloatMatrix): FloatArray = FloatArray(3) { i ->
    rot[0][i] * v[0] + rot[1][i] * v[1] + rot[2][i] * v[2]
}

/** Composes two rotations: res = m followed by rot. */
fun rotateRotation(m: FloatMatrix, rot: FloatMatrix): FloatMatrix {
    val res = identityFloatMatrix()
    for (i in 0..2) for (j in 0..2) {
        var sum = 0f
        for (k in 0..2) sum += rot[k][j] * m[i][k]
        res[i][j] = sum
    }
    return res
}

fun rotateIntegerVector(v: IntArray, rot: IntMatrix): IntArray = IntArray(3) { i ->
    rot[0][i] * v[0] + rot[1][i] * v[1] + rot[2][i] * v[2]
}

/** Rotates [m] in place by 90 degrees * sign around [axis] (0=X, 1=Y, 2=Z). */
fun rotateIntegerRotation(m: IntMatrix, axis: Int, sign: Int) {
    val m1 = Array(3) { i -> FloatArray(3) { j -> m[i][j].toFloat() } }
    val angle = FloatArray(3)
    angle[axis] = sign * (PI / 2).toFloat()
    val m2 = createRotation(angle)
    val m3 = rotateRotation(m1, m2)
    for (i in 0..2) for (j in 0..2) {
        val v = m3[i][j]
        m[i][j] = (if (v < 0) -1 else 1) * (if (abs(v) > 0.5f) 1 else 0)
    }
}

fun inverseIntegerRotation(m: IntMatrix): IntMatrix = Array(3) { i -> IntArray(3) { j -> m[j][i] } }
