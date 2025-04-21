package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Cube(private val x: Float, private val y: Float, private val z: Float) {

    private val vertexBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(0.2, 0.8, 1.0, 1.0);
        }
    """

    private val program: Int

    private val cubeCoords = floatArrayOf(
        // Front face
        -0.5f,  0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f,
        0.5f, -0.5f, 0.5f,
        0.5f,  0.5f, 0.5f,

        // Back face
        -0.5f,  0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f,
        0.5f,  0.5f, -0.5f,
    )

    private val drawOrder = shortArrayOf(
        0, 1, 2, 0, 2, 3, // front
        4, 5, 6, 4, 6, 7, // back
        0, 4, 7, 0, 7, 3, // top
        1, 5, 6, 1, 6, 2, // bottom
        0, 1, 5, 0, 5, 4, // left
        3, 2, 6, 3, 6, 7  // right
    )

    private val indexBuffer = ByteBuffer
        .allocateDirect(drawOrder.size * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .apply {
            put(drawOrder)
            position(0)
        }

    init {
        val bb = ByteBuffer.allocateDirect(cubeCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer().apply {
            put(cubeCoords)
            position(0)
        }

        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = glCreateProgram().also {
            glAttachShader(it, vertexShader)
            glAttachShader(it, fragmentShader)
            glLinkProgram(it)
        }
    }

    fun draw(vpMatrix: FloatArray) {
        glUseProgram(program)

        val positionHandle = glGetAttribLocation(program, "vPosition")
        val mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")

        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 3 * 4, vertexBuffer)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, z, y)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, indexBuffer)

        glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also { shader ->
            glShaderSource(shader, shaderCode)
            glCompileShader(shader)
        }
    }
}
