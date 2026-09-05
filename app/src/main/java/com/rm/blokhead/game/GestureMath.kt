package com.rm.blokhead.game

import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Pure step/remainder accumulator shared by every axis the gesture control scheme tracks (drag
 * distance for movement, pan distance for X/Y rotation, twist angle for Z rotation): given how
 * far a continuous drag/pan/twist has accumulated since the last fired step and a fixed
 * [stepSize], returns how many whole steps have now been crossed (signed, since [accumulated] can
 * be negative) and the leftover remainder to keep accumulating from on the next call — so holding
 * a drag/pan/twist past multiple step widths in one gesture repeats that many actions, and a
 * released/restarted gesture always starts a fresh accumulation from zero.
 */
fun stepsFromAccumulated(accumulated: Float, stepSize: Float): Pair<Int, Float> {
    val steps = (accumulated.absoluteValue / stepSize).toInt() * accumulated.sign.toInt()
    val remainder = accumulated - steps * stepSize
    return steps to remainder
}
