package com.rm.blokhead.render

import android.opengl.GLES20

private const val VERTEX_SHADER = """
    uniform mat4 uMVPMatrix;
    attribute vec4 aPosition;
    attribute vec4 aColor;
    varying vec4 vColor;
    void main() {
        gl_Position = uMVPMatrix * aPosition;
        vColor = aColor;
    }
"""

private const val FRAGMENT_SHADER = """
    precision mediump float;
    varying vec4 vColor;
    void main() {
        gl_FragColor = vColor;
    }
"""

/** Minimal GLES2 replacement for the original's fixed-function pipeline: a single flat-shaded,
 *  per-vertex-color program used for both the well grid and the cubes. Lighting is baked into
 *  vertex colors on the CPU (see [Geometry]) rather than computed in the shader, since every
 *  face is a flat quad and doesn't need more. */
class ShaderProgram {
    val programId: Int = GLES20.glCreateProgram().also { program ->
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
    }

    val positionAttribute = GLES20.glGetAttribLocation(programId, "aPosition")
    val colorAttribute = GLES20.glGetAttribLocation(programId, "aColor")
    val mvpMatrixUniform = GLES20.glGetUniformLocation(programId, "uMVPMatrix")

    fun use() = GLES20.glUseProgram(programId)

    private fun compileShader(type: Int, source: String): Int =
        GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
        }
}
