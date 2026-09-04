package com.rm.blokhead.render

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.rm.blokhead.game.GameEngine
import kotlin.math.PI

/** Hosts [BlokoutRenderer] and translates single-finger drags into camera orbit, standing in for
 *  the original's mouse-drag view control (ctrlMouse/ctrlMotion in control.c). Piece movement
 *  input lands in a later pass. */
class BlokoutSurfaceView(context: Context, engine: GameEngine) : GLSurfaceView(context) {

    private val renderer = BlokoutRenderer(engine)
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                renderer.orbitYaw -= dx / width * PI.toFloat()
                renderer.orbitPitch = (renderer.orbitPitch + dy / height * PI.toFloat())
                    .coerceIn(0.15f, PI.toFloat() / 2f - 0.05f)
                lastTouchX = event.x
                lastTouchY = event.y
            }
        }
        return true
    }
}
