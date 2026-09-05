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
import kotlinx.coroutines.withTimeoutOrNull

/** One-finger drag distance (in dp) that fires a single move — roughly 2/3 of a control button's
 *  width, so a deliberate drag reads as one distinct step rather than needing a huge swipe. */
private val MOVE_STEP = 32.dp

/** Two-finger pan distance (in dp) that fires a single X/Y rotate. Slightly larger than
 *  [MOVE_STEP] since it's measured on a two-finger centroid, which drifts more than one fingertip. */
private val ROTATE_PAN_STEP = 40.dp

/** Two-finger twist angle (in degrees) that fires a single Z rotate — big enough that ordinary
 *  hand jitter while panning/pinching doesn't cross it by accident, small enough that a
 *  deliberate twist yields a few steps. */
private const val ROTATE_TWIST_STEP_DEGREES = 25f

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

    // Two (or more) fingers down: interpret the pinch centroid's pan as X/Y rotate and the pair's
    // angle as a Z twist, for as long as at least 2 fingers stay down.
    suspend fun AwaitPointerEventScope.runRotateLoop(initialChanges: List<PointerInputChange>) {
        var (lastCentroid, lastAngle) = centroidAndAngleDegrees(initialChanges[0].position, initialChanges[1].position)
        var panAccumX = 0f
        var panAccumY = 0f
        var twistAccumDegrees = 0f
        while (true) {
            val pressed = awaitPointerEvent().changes.filter { it.pressed }
            if (pressed.size < 2) return
            val (centroid, angle) = centroidAndAngleDegrees(pressed[0].position, pressed[1].position)
            panAccumX += centroid.x - lastCentroid.x
            panAccumY += centroid.y - lastCentroid.y
            var angleDelta = angle - lastAngle
            if (angleDelta > 180f) angleDelta -= 360f
            if (angleDelta < -180f) angleDelta += 360f
            twistAccumDegrees += angleDelta
            lastCentroid = centroid
            lastAngle = angle
            panAccumX = fireSteps(panAccumX, rotatePanStepPx) { sign -> onRotate(Axis.Y, sign) }
            panAccumY = fireSteps(panAccumY, rotatePanStepPx) { sign -> onRotate(Axis.X, -sign) }
            twistAccumDegrees = fireSteps(twistAccumDegrees, ROTATE_TWIST_STEP_DEGREES) { sign -> onRotate(Axis.Z, sign) }
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
