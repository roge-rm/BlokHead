package com.rm.blokhead.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rm.blokhead.game.Axis
import com.rm.blokhead.game.stepsFromAccumulated
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlinx.coroutines.withTimeoutOrNull

/** One-finger drag distance (in dp) that fires a single move — roughly 2/3 of a control button's
 *  width, so a deliberate drag reads as one distinct step rather than needing a huge swipe. */
private val MOVE_STEP = 32.dp

/** Two-finger pan distance (in dp) that fires a single X/Y rotate once a gesture has committed to
 *  panning (see [ROTATE_PAN_COMMIT]/[ROTATE_TWIST_COMMIT_DEGREES]) — bigger than [MOVE_STEP]
 *  since it's measured on a two-finger centroid, which drifts more than one fingertip, and needs
 *  a deliberately large motion per rotate so a two-finger gesture doesn't spin the piece many
 *  times over from an ordinary amount of hand movement. */
private val ROTATE_PAN_STEP = 72.dp

/** Two-finger twist angle (in degrees) that fires a single Z rotate once a gesture has committed
 *  to twisting — likewise sized so a natural twist yields just a couple of steps, not a dozen. */
private const val ROTATE_TWIST_STEP_DEGREES = 45f

/** How far a two-finger gesture's pan/twist has to travel before it *commits* to being a pan or
 *  a twist gesture for the rest of that gesture (whichever crosses its own threshold first) —
 *  much smaller than the step sizes above, just enough to tell intent apart. Once committed, the
 *  other axis is ignored entirely: without this, an imprecise real-world twist (which naturally
 *  drifts the centroid a little) or an imprecise pan (which naturally rotates the finger pair a
 *  little) would fire both X/Y *and* Z rotates from the same motion, compounding into the piece
 *  spinning far more than intended. */
private val ROTATE_PAN_COMMIT = 16.dp
private const val ROTATE_TWIST_COMMIT_DEGREES = 12f

private fun centroidAndAngleDegrees(a: Offset, b: Offset): Pair<Offset, Float> {
    val centroid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val angle = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
    return centroid to angle
}

private fun fireSteps(accumulated: Float, stepSize: Float, onStep: (sign: Int) -> Unit): Float {
    val (steps, remainder) = stepsFromAccumulated(accumulated, stepSize)
    repeat(abs(steps)) { onStep(if (steps > 0) 1 else -1) }
    return remainder
}

/**
 * Direct-touch alternative to [GameControls]' on-screen buttons (see the `gestureControlsEnabled`
 * setting): everything happens as gestures on the grid itself, freeing the whole screen for it.
 *
 * - One-finger drag moves the piece (repeats while held, one step per [MOVE_STEP] crossed).
 * - Two-finger drag rotates X/Y — horizontal pan → Y (matches [RotateCluster]'s horizontal Y
 *   buttons), vertical pan → X (matches its vertical X buttons).
 * - Two-finger twist rotates Z — the "secondary" axis gets the "secondary" gesture, mirroring the
 *   existing button/gamepad hierarchy (X/Y on the primary reachable inputs, Z on a secondary one).
 * - A plain tap toggles pause, same as the button scheme's tap-to-pause; a long-press (cancelled
 *   by any movement past touch slop, or by a second finger joining — exactly like a normal
 *   Android long-press) hard-drops instead. Long-press was chosen over double-tap so there's no
 *   delayed "was that a single or double tap?" resolution racing against the pause toggle.
 *
 * Every callback here is one of `GameScreen`'s existing move/rotate/hard-drop/pause lambdas —
 * this modifier only detects gestures and translates them into those same calls; the underlying
 * `GameEngine` already no-ops all of them while paused/game-over, so nothing here needs to check
 * that itself (except [onTogglePause], which decides on its own whether pausing still means
 * anything).
 */
fun Modifier.gestureControls(
    onMove: (axis: Int, sign: Int) -> Unit,
    onRotate: (axis: Int, sign: Int) -> Unit,
    onHardDrop: () -> Unit,
    onTogglePause: () -> Unit,
): Modifier = pointerInput(Unit) {
    val moveStepPx = MOVE_STEP.toPx()
    val rotatePanStepPx = ROTATE_PAN_STEP.toPx()
    val rotatePanCommitPx = ROTATE_PAN_COMMIT.toPx()

    // Two (or more) fingers down: interpret the pinch centroid's pan as X/Y rotate and the pair's
    // angle as a Z twist, for as long as at least 2 fingers stay down. A gesture commits to
    // exactly one of pan/twist (whichever crosses its own small commit threshold first) and
    // ignores the other for the rest of the gesture — seeing both at once, uncommitted, would
    // double-fire from a single real-world motion (see the constants' doc comments above).
    suspend fun AwaitPointerEventScope.runRotateLoop(initialChanges: List<PointerInputChange>) {
        var (lastCentroid, lastAngle) = centroidAndAngleDegrees(initialChanges[0].position, initialChanges[1].position)
        var panAccumX = 0f
        var panAccumY = 0f
        var twistAccumDegrees = 0f
        var committedToPan = false
        var committedToTwist = false
        while (true) {
            val pressed = awaitPointerEvent().changes.filter { it.pressed }
            if (pressed.size < 2) return
            val (centroid, angle) = centroidAndAngleDegrees(pressed[0].position, pressed[1].position)
            val panDeltaX = centroid.x - lastCentroid.x
            val panDeltaY = centroid.y - lastCentroid.y
            var angleDelta = angle - lastAngle
            if (angleDelta > 180f) angleDelta -= 360f
            if (angleDelta < -180f) angleDelta += 360f
            lastCentroid = centroid
            lastAngle = angle

            if (!committedToPan && !committedToTwist) {
                panAccumX += panDeltaX
                panAccumY += panDeltaY
                twistAccumDegrees += angleDelta
                val panDistance = hypot(panAccumX, panAccumY)
                if (panDistance > rotatePanCommitPx) {
                    committedToPan = true
                    twistAccumDegrees = 0f
                } else if (abs(twistAccumDegrees) > ROTATE_TWIST_COMMIT_DEGREES) {
                    committedToTwist = true
                    panAccumX = 0f
                    panAccumY = 0f
                }
            } else if (committedToPan) {
                panAccumX += panDeltaX
                panAccumY += panDeltaY
            } else {
                twistAccumDegrees += angleDelta
            }

            if (committedToPan) {
                panAccumX = fireSteps(panAccumX, rotatePanStepPx) { sign -> onRotate(Axis.Y, sign) }
                panAccumY = fireSteps(panAccumY, rotatePanStepPx) { sign -> onRotate(Axis.X, -sign) }
            } else if (committedToTwist) {
                twistAccumDegrees = fireSteps(twistAccumDegrees, ROTATE_TWIST_STEP_DEGREES) { sign -> onRotate(Axis.Z, sign) }
            }
            pressed.forEach { it.consume() }
        }
    }

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        // Phase 1: undecided — a single finger that hasn't yet crossed the move threshold. Races
        // real elapsed time against new pointer activity, since a perfectly still finger produces
        // no further events on its own and would otherwise never let a long-press resolve.
        var lastEventUptime = down.uptimeMillis
        var moveAccumX = 0f
        var moveAccumY = 0f
        var resolvedToMove = false
        while (!resolvedToMove) {
            val deadline = down.uptimeMillis + viewConfiguration.longPressTimeoutMillis
            val remaining = deadline - lastEventUptime
            if (remaining <= 0) {
                onHardDrop()
                return@awaitEachGesture
            }
            val event = withTimeoutOrNull(remaining) { awaitPointerEvent() }
            if (event == null) {
                onHardDrop()
                return@awaitEachGesture
            }
            val pressed = event.changes.filter { it.pressed }
            if (pressed.isEmpty()) {
                onTogglePause()
                return@awaitEachGesture
            }
            lastEventUptime = pressed.maxOf { it.uptimeMillis }
            if (pressed.size >= 2) {
                runRotateLoop(pressed)
                return@awaitEachGesture
            }
            val change = pressed.first()
            val delta = change.position - change.previousPosition
            moveAccumX += delta.x
            moveAccumY += delta.y
            if (abs(moveAccumX) > viewConfiguration.touchSlop || abs(moveAccumY) > viewConfiguration.touchSlop) {
                resolvedToMove = true
            }
        }

        // Phase 2: move — one finger, already past the initial threshold. Independent X/Y
        // accumulators so a mostly-diagonal drag can still fire both axes as each crosses its own
        // threshold, rather than picking one "dominant" axis for the whole gesture.
        while (true) {
            val pressed = awaitPointerEvent().changes.filter { it.pressed }
            if (pressed.isEmpty()) return@awaitEachGesture
            if (pressed.size >= 2) {
                runRotateLoop(pressed)
                return@awaitEachGesture
            }
            val change = pressed.first()
            val delta = change.position - change.previousPosition
            change.consume()
            moveAccumX += delta.x
            moveAccumY += delta.y
            moveAccumX = fireSteps(moveAccumX, moveStepPx) { sign -> onMove(Axis.X, sign) }
            moveAccumY = fireSteps(moveAccumY, moveStepPx) { sign -> onMove(Axis.Y, -sign) }
        }
    }
}
