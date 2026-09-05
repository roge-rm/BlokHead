package com.rm.blokhead.game

/** A queued 90-degree turn, ported from blokout's TurnInfo — lets rotation animate smoothly
 *  while grid logic already treats the turn as committed. */
data class TurnInfo(val startTime: Float, val axis: Int, val sign: Int)

/**
 * The active falling piece, ported from blokout's Block (blocks.c/blocks.h). All times are
 * seconds elapsed since this block spawned (the original reset a per-block Timer on creation,
 * so every timestamp threaded through Block/Collision is relative to that spawn, not wall time).
 */
class Block(val form: Form, fallSpeed: Float = 0.1f) {
    val isMoving = BooleanArray(2)
    val lastMove = FloatArray(2)
    val lastPosition = FloatArray(2)

    /** Current continuous position; X/Y animate towards [targetPosition], Z falls continuously. */
    val position = FloatArray(3)
    val targetPosition = IntArray(2)

    val lastOrientation: IntMatrix = identityIntMatrix()
    var orientation: FloatMatrix = identityFloatMatrix()
    val targetOrientation: IntMatrix = identityIntMatrix()
    private val turns = ArrayDeque<TurnInfo>()

    var lastFall = 0f
    var lastStop = 0f

    // The original starts this at -5 (see blocks.c's createBlock()) as a "fast catch-up" hack:
    // tryLowerBlock's very first call jumps position[2] straight to this value before normal
    // per-frame fall speed takes over, saving a slow fall through a very tall well before the
    // piece is even visible. Our wells are much shallower, so that same fixed jump eats a much
    // bigger fraction of the visible depth — the piece would visibly start already partway down
    // the tunnel instead of at its front opening. Starting at 0 (matching position[2]) instead
    // makes the very first fall tick behave like every other: a smooth, continuous descent from
    // the spawn point.
    var stopHeight = 0f

    var fallSpeed = fallSpeed
    var moveSpeed = 2f
    var turnSpeed = 3f

    init {
        targetPosition[0] = form.centerPoint[0]
        targetPosition[1] = form.centerPoint[1]
        position[0] = targetPosition[0].toFloat()
        position[1] = targetPosition[1].toFloat()
        position[2] = 0f
        lastPosition[0] = position[0]
        lastPosition[1] = position[1]
    }

    /** Advances X/Y move animation and orientation towards their targets. Ported from updateBlock(). */
    fun update(nowTime: Float) {
        for (axis in 0..1) {
            if (!isMoving[axis]) continue
            val move = (nowTime - lastMove[axis]) * moveSpeed
            if (move >= kotlin.math.abs(lastPosition[axis] - targetPosition[axis])) {
                lastPosition[axis] = targetPosition[axis].toFloat()
                position[axis] = targetPosition[axis].toFloat()
                isMoving[axis] = false
            } else {
                position[axis] = if (targetPosition[axis] < lastPosition[axis]) {
                    lastPosition[axis] - move
                } else {
                    lastPosition[axis] + move
                }
            }
        }

        // Retire turns whose animation has finished, folding them into lastOrientation.
        var finished = 0
        while (finished < turns.size && nowTime - turns[finished].startTime >= 1f / turnSpeed) {
            val turn = turns[finished]
            rotateIntegerRotation(lastOrientation, turn.axis, turn.sign)
            finished++
        }
        repeat(finished) { turns.removeFirst() }

        var orient: FloatMatrix = Array(3) { i -> FloatArray(3) { j -> lastOrientation[i][j].toFloat() } }
        for (turn in turns) {
            val angle = FloatArray(3)
            angle[turn.axis] = (nowTime - turn.startTime) * turnSpeed * turn.sign * kotlin.math.PI.toFloat() / 2f
            orient = rotateRotation(orient, createRotation(angle))
        }
        orientation = orient
    }

    /** Starts animating [axis] towards a new integer target one step in [sign]'s direction. */
    fun moveTo(axis: Int, sign: Int, nowTime: Float) {
        lastPosition[axis] = position[axis]
        lastMove[axis] = nowTime
        targetPosition[axis] += sign
        isMoving[axis] = true
    }

    /** Commits a 90-degree turn around [axis] to [targetOrientation] and queues its animation. */
    fun turn(axis: Int, sign: Int, nowTime: Float) {
        turns.addLast(TurnInfo(nowTime, axis, sign))
        rotateIntegerRotation(targetOrientation, axis, sign)
    }

    fun dimensions(orientation: IntMatrix = targetOrientation): IntArray =
        rotateIntegerVector(form.dimensions, orientation).map { kotlin.math.abs(it) }.toIntArray()

    fun center(dim: IntArray, orientation: IntMatrix = targetOrientation): IntArray {
        val cp2 = IntArray(3) { form.centerPoint[it] + 1 }
        val cp = rotateIntegerVector(cp2, orientation)
        return IntArray(3) { i -> if (cp[i] < 0) cp[i] + dim[i] else cp[i] - 1 }
    }

    /** Whether the piece has a cube at local grid [pos], as seen under [orientation]. */
    fun cubeAt(pos: IntArray, orientation: IntMatrix = targetOrientation): Boolean {
        val rdim = dimensions(orientation)
        val p = IntArray(3) { pos[it] + 1 }
        val inverse = inverseIntegerRotation(orientation)
        val rp = rotateIntegerVector(p, inverse)
        val rpos = IntArray(3) { i -> if (rp[i] < 0) rp[i] + form.dimensions[i] else rp[i] - 1 }
        for (i in 0..2) check(rpos[i] in 0 until form.dimensions[i]) {
            "cubeAt out of range: rpos=${rpos.toList()} dim=${form.dimensions.toList()} " +
                "rdim=${rdim.toList()} pos=${pos.toList()}"
        }
        return form.cubeAt(rpos[0], rpos[1], rpos[2]) != 0
    }
}
