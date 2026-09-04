package com.rm.blokhead.render

import android.content.Context
import android.opengl.GLSurfaceView
import com.rm.blokhead.game.GameEngine

/** Hosts [BlokoutRenderer], which renders a fixed top-down camera (see its doc). Piece movement
 *  input arrives via [enqueue], called from the Compose control overlay in MainActivity — the
 *  original's mouse-drag view control (ctrlMouse/ctrlMotion in control.c) has no equivalent here
 *  since the camera no longer orbits. */
class BlokoutSurfaceView(context: Context, engine: GameEngine) : GLSurfaceView(context) {

    private val renderer = BlokoutRenderer(engine)

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    /** Queues a [GameEngine] action to run on the GL thread on the next frame. */
    fun enqueue(action: GameEngine.() -> Unit) = renderer.enqueue(action)
}
