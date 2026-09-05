package com.rm.blokhead.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rm.blokhead.game.Axis
import com.rm.blokhead.game.stepsFromAccumulated
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlinx.coroutines.withTimeoutOrNull

/** Each in-game rotate step turns the piece a quarter turn — so a two-finger *twist* fires one
 *  rotate per 90° the fingers themselves have physically twisted, the same direct real-world
 *  correspondence [gestureControls]' `cellSize` gives one-finger move: twist your fingers a
 *  quarter turn, the piece turns a quarter turn, at whatever speed you actually twisted at. */
private const val ROTATE_TWIST_STEP_DEGREES = 90f

/** Two-finger *pan* (translation, not twist) fires X/Y rotates instead of Z — there's no rotation
 *  angle to match 1:1 here since panning isn't a rotation gesture, so it reuses the same
 *  real-world [gestureControls] `cellSize` unit move already does (one cell of pan = one rotate),
 *  rather than a distance disconnected from anything else on screen. */
private const val ROTATE_PAN_STEP_CELLS = 1f

/** How far a two-finger gesture's pan/twist has to travel before it *commits* to being a pan or
 *  a twist gesture for the rest of that gesture (whichever crosses its own threshold first) — a
 *  quarter of each's full step, just enough to tell intent apart well before either could
 *  actually fire. Once committed, the other axis is ignored entirely: without this, an imprecise
 *  real-world twist (which naturally drifts the centroid a little) or an imprecise pan (which
 *  naturally rotates the finger pair a little) would fire both X/Y *and* Z rotates from the same
 *  motion, compounding into the piece spinning far more than intended. */
private const val ROTATE_PAN_COMMIT_CELLS = ROTATE_PAN_STEP_CELLS / 4f
private const val ROTATE_TWIST_COMMIT_DEGREES = ROTATE_TWIST_STEP_DEGREES / 4f

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
 * - One-finger drag moves the piece exactly as far as the finger has physically moved, in real
 *   time: every [cellSize] of on-screen drag distance moves the piece one space, matching however
 *   many cells wide/deep the rendered grid actually is on screen (the caller passes this in from
 *   the same geometry it used to size/place the grid) rather than an arbitrary fixed distance —
 *   so dragging your finger across what visually reads as one square really does move it exactly
 *   one square, however fast or slow that drag happens. There's deliberately no other notion of
 *   "speed" or acceleration beyond that direct correspondence — a fast flick just crosses more
 *   cell-widths in the same real time, exactly as many as it visually crossed.
 * - Two-finger drag rotates X/Y, one quarter turn per [cellSize] of pan (reusing move's own real
 *   unit rather than an unrelated fixed distance) — horizontal pan → Y (matches [RotateCluster]'s
 *   horizontal Y buttons), vertical pan → X (matches its vertical X buttons).
 * - Two-finger twist rotates Z, one quarter turn per 90° the fingers themselves twist — the same
 *   direct real-world correspondence as move/pan, just in angle instead of distance since a twist
 *   is a rotation gesture already. Twist gets the "secondary" axis (Z) rather than pan, mirroring
 *   the existing button/gamepad hierarchy (X/Y on the primary reachable inputs, Z on a secondary
 *   one).
 * - A long-press (cancelled by any movement past touch slop, or by a second finger joining —
 *   exactly like a normal Android long-press) hard-drops. A plain tap toggles pause only if it
 *   started in the black border area not covered by the rendered grid (per [isInPauseZone]) — a
 *   tap on the grid content itself does nothing.
 *
 * This single modifier spans the *entire* touch surface (grid content plus any surrounding
 * border) rather than being layered under a separate border-only tap element: Android/Compose
 * hands an entire gesture to whichever element first hit-tested its initial touch-down, so a
 * drag that happened to start a few pixels into the border would otherwise be swallowed by that
 * separate element even once the finger moves well into the grid — [isInPauseZone] lets one
 * shared surface still tell the two areas apart for tap purposes without that capture problem.
 *
 * Every other callback here is one of `GameScreen`'s existing move/rotate/hard-drop lambdas —
 * this modifier only detects gestures and translates them into those same calls; the underlying
 * `GameEngine` already no-ops all of them while paused/game-over, so nothing here needs to check
 * that itself (except [onTogglePause], which decides on its own whether pausing still means
 * anything).
 */
fun Modifier.gestureControls(
    onMove: (axis: Int, sign: Int) -> Unit,
    onRotate: (axis: Int, sign: Int) -> Unit,
    onHardDrop: () -> Unit,
    cellSize: Dp,
    isInPauseZone: (position: Offset) -> Boolean = { false },
    onTogglePause: () -> Unit = {},
): Modifier = pointerInput(Unit) {
    val moveStepPx = cellSize.toPx()
    val rotatePanStepPx = moveStepPx * ROTATE_PAN_STEP_CELLS
    val rotatePanCommitPx = moveStepPx * ROTATE_PAN_COMMIT_CELLS

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

        // Phase 1: undecided — a single finger that hasn't yet moved enough to count as a drag
        // (system touch slop, not [cellSize] — just enough to tell a drag apart from a tap/long-
        // press, well before a whole cell of movement). Races real elapsed time against new
        // pointer activity, since a perfectly still finger produces no further events on its own
        // and would otherwise never let a long-press resolve.
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
                // A plain tap — only meaningful in the border area (see the doc comment above).
                if (isInPauseZone(down.position)) onTogglePause()
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

        // Phase 2: move — the piece tracks the finger directly from here on. Independent X/Y
        // accumulators so a mostly-diagonal drag can still fire both axes as each crosses its own
        // [cellSize] threshold, rather than picking one "dominant" axis for the whole gesture; a
        // second finger can still hand off to rotate mid-drag.
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
