package com.rm.blokhead.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.rm.blokhead.game.GameEngine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * GLES2 replacement for the original's fixed-function OpenGL 1.x rendering (transforms.c,
 * texture.c, image.c and the GL calls in tube.c/blocks.c/game.c) — the immediate-mode/matrix-
 * stack API those relied on doesn't exist on GLES2+, so this redraws the same well + falling
 * piece with a small shader-based pipeline instead. Textured walls are dropped for now (solid
 * colors only); see the project plan's polish phase.
 *
 * The camera looks straight down the well's long axis from above (like blockout.net's
 * BlockOut II reference), rather than the original's freely-orbited oblique view — fixed, no
 * touch-drag.
 *
 * Drives the game loop itself: [onDrawFrame] measures the real time elapsed since the previous
 * frame and advances [engine] by that delta before rendering, since GLSurfaceView's continuous
 * render mode already gives us a steady per-frame callback.
 */
class BlokoutRenderer(private val engine: GameEngine) : GLSurfaceView.Renderer {

    private lateinit var shader: ShaderProgram
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var lastFrameTimeNanos = 0L

    private val wellLineColor = floatArrayOf(0.85f, 0.85f, 0.1f, 1f)
    // The falling piece is drawn as a wireframe only (no filled faces) so the grid and any locked
    // cubes beneath it stay fully visible through it, matching BlockOut II's reference look.
    private val blockWireColor = floatArrayOf(1f, 1f, 1f, 1f)

    // Compose controls run on the UI thread but GameEngine isn't synchronized, so input is
    // queued here and drained on the GL thread at the start of onDrawFrame, right before
    // engine.update() — the same thread that owns all other engine mutation.
    private val pendingActions = ConcurrentLinkedQueue<GameEngine.() -> Unit>()

    fun enqueue(action: GameEngine.() -> Unit) {
        pendingActions.add(action)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.03f, 0.04f, 0.045f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        shader = ShaderProgram()
        lastFrameTimeNanos = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        while (true) {
            val action = pendingActions.poll() ?: break
            engine.action()
        }

        val now = System.nanoTime()
        val delta = if (lastFrameTimeNanos == 0L) 0f else (now - lastFrameTimeNanos) / 1_000_000_000f
        lastFrameTimeNanos = now
        engine.update(delta.coerceAtMost(0.1f)) // clamp to avoid a huge jump after a stall

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        setUpCamera()
        shader.use()

        drawWellGrid()
        drawLockedCubes()
        drawFallingBlock()
    }

    private fun setUpCamera() {
        val tube = engine.tube
        val width = tube.dimensions[0].toFloat()
        val depth = tube.dimensions[1].toFloat()
        val height = tube.dimensions[2].toFloat()
        val aspect = viewportWidth.toFloat() / viewportHeight.toFloat()
        // A piece spawns right at the well's top opening, so the eye needs real standoff above it
        // — otherwise a freshly spawned piece sits well under one world unit from the camera and
        // fills the whole frame. This margin scales with the footprint so it stays proportional
        // for a wider/narrower well.
        val standoff = maxOf(width, depth) * 3f
        val eyeZ = height + standoff

        Matrix.perspectiveM(projectionMatrix, 0, 55f, aspect, 0.5f, eyeZ + 5f)

        val cx = width / 2f
        val cy = depth / 2f
        // Eye looks straight down the well's long axis from above — the walls recede to a
        // vanishing point at the floor, same as the reference's fixed view.
        Matrix.setLookAtM(viewMatrix, 0, cx, cy, eyeZ, cx, cy, 0f, 0f, 1f, 0f)
    }

    private fun drawLines(vertexData: FloatArray, mvp: FloatArray) {
        val buffer = vertexData.toDirectBuffer()
        GLES20.glVertexAttribPointer(shader.positionAttribute, 3, GLES20.GL_FLOAT, false, FLOATS_PER_CUBE_VERTEX * 4, buffer)
        GLES20.glEnableVertexAttribArray(shader.positionAttribute)
        buffer.position(3)
        GLES20.glVertexAttribPointer(shader.colorAttribute, 4, GLES20.GL_FLOAT, false, FLOATS_PER_CUBE_VERTEX * 4, buffer)
        GLES20.glEnableVertexAttribArray(shader.colorAttribute)
        GLES20.glUniformMatrix4fv(shader.mvpMatrixUniform, 1, false, mvp, 0)
        GLES20.glLineWidth(2f)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexData.size / FLOATS_PER_CUBE_VERTEX)
    }

    private fun drawTriangles(vertexData: FloatArray, mvp: FloatArray) {
        if (vertexData.isEmpty()) return
        val buffer = vertexData.toDirectBuffer()
        GLES20.glVertexAttribPointer(shader.positionAttribute, 3, GLES20.GL_FLOAT, false, FLOATS_PER_CUBE_VERTEX * 4, buffer)
        GLES20.glEnableVertexAttribArray(shader.positionAttribute)
        buffer.position(3)
        GLES20.glVertexAttribPointer(shader.colorAttribute, 4, GLES20.GL_FLOAT, false, FLOATS_PER_CUBE_VERTEX * 4, buffer)
        GLES20.glEnableVertexAttribArray(shader.colorAttribute)
        GLES20.glUniformMatrix4fv(shader.mvpMatrixUniform, 1, false, mvp, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexData.size / FLOATS_PER_CUBE_VERTEX)
    }

    private fun drawWellGrid() {
        val tube = engine.tube
        val lines = Geometry.buildWellGridLines(tube.dimensions[0], tube.dimensions[1], tube.dimensions[2], wellLineColor)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        drawLines(lines.toFloatArray(), mvpMatrix)
    }

    private fun drawLockedCubes() {
        val tube = engine.tube
        val vertices = ArrayList<Float>()
        for (z in 0 until tube.dimensions[2]) {
            val color = colorForLayer(z)
            for (y in 0 until tube.dimensions[1]) for (x in 0 until tube.dimensions[0]) {
                if (tube.isFilled(x, y, z)) {
                    Geometry.appendCube(vertices, floatArrayOf(x.toFloat(), y.toFloat(), z.toFloat()), color)
                }
            }
        }
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        drawTriangles(vertices.toFloatArray(), mvpMatrix)
    }

    private fun drawFallingBlock() {
        val block = engine.currentBlock
        val form = block.form
        val vertices = ArrayList<Float>()
        for (z in 0 until form.dimensions[2]) for (y in 0 until form.dimensions[1]) for (x in 0 until form.dimensions[0]) {
            if (form.cubeAt(x, y, z) != 0) {
                val local = floatArrayOf(
                    x - form.centerPoint[0] - 0.5f,
                    y - form.centerPoint[1] - 0.5f,
                    z - form.centerPoint[2] - 0.5f,
                )
                Geometry.appendCubeWireframe(vertices, local, blockWireColor)
            }
        }

        // Model matrix: rotate by the block's continuous orientation, then translate to its
        // continuous position (matching the original's per-frame model matrix in gameDisplay()).
        // block.position[2] follows the original's convention of 0 = the well's top opening,
        // falling towards negative values; the locked-cube grid here instead runs 0 = bottom
        // upward (matching Tube's array layout), so the Z position is re-based by the well's
        // full height to land in that same space.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(
            modelMatrix, 0,
            block.position[0] + 0.5f,
            block.position[1] + 0.5f,
            block.position[2] + engine.tube.dimensions[2] + 0.5f,
        )
        val rotation = floatArrayOf(
            block.orientation[0][0], block.orientation[0][1], block.orientation[0][2], 0f,
            block.orientation[1][0], block.orientation[1][1], block.orientation[1][2], 0f,
            block.orientation[2][0], block.orientation[2][1], block.orientation[2][2], 0f,
            0f, 0f, 0f, 1f,
        )
        Matrix.multiplyMM(tempMatrix, 0, modelMatrix, 0, rotation, 0)
        System.arraycopy(tempMatrix, 0, modelMatrix, 0, 16)

        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        drawLines(vertices.toFloatArray(), mvpMatrix)
    }

    private fun colorForLayer(z: Int): FloatArray {
        // Deterministic per-layer hue, standing in for the original's randomized layerMaterials.
        val hue = (z * 47) % 360
        return hsvToRgba(hue.toFloat(), 0.55f, 0.85f)
    }
}

private fun hsvToRgba(h: Float, s: Float, v: Float): FloatArray {
    val c = v * s
    val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1))
    val m = v - c
    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return floatArrayOf(r + m, g + m, b + m, 1f)
}

private fun FloatArray.toDirectBuffer(): FloatBuffer =
    ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().also {
        it.put(this)
        it.position(0)
    }
