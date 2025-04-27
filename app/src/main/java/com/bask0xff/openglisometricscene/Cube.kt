package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

class Cube(val x: Float, val y: Float, val z: Float, private val baseColor: FloatArray) {

    private var color: FloatArray = baseColor.copyOf()
    private val vertexBuffer: FloatBuffer
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val program: Int

    private val _cubeSize = 0.1f;
    private val cubeHeight = 0.1f;

    fun cubeSize(): Float {
        return _cubeSize
    }

    private val cubeCoords = floatArrayOf(
        -_cubeSize, _cubeSize, cubeHeight,
        -_cubeSize, -_cubeSize, cubeHeight,
        _cubeSize, -_cubeSize, cubeHeight,
        _cubeSize, _cubeSize, cubeHeight,
        -_cubeSize, _cubeSize, -cubeHeight,
        -_cubeSize, -_cubeSize, -cubeHeight,
        _cubeSize, -_cubeSize, -cubeHeight,
        _cubeSize, _cubeSize, -cubeHeight
    )

    private val drawOrder = shortArrayOf(
        0,1,2, 0,2,3,  4,5,6, 4,6,7,
        0,4,7, 0,7,3,  1,5,6, 1,6,2,
        0,1,5, 0,5,4,  3,2,6, 3,6,7
    )

    init {
        val bb = ByteBuffer.allocateDirect(cubeCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(cubeCoords)
        vertexBuffer.position(0)

        val vertexShader = loadShader(GL_VERTEX_SHADER, """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """)

        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, """
            precision mediump float;
            uniform vec4 uColor;
            void main() {
                gl_FragColor = uColor;
            }
        """)

        program = glCreateProgram().also {
            glAttachShader(it, vertexShader)
            glAttachShader(it, fragmentShader)
            glLinkProgram(it)
        }
    }

    fun draw(vpMatrix: FloatArray) {
        val offset = 1f // 2/3f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x * offset, y * offset, z * offset)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        glUseProgram(program)

        val positionHandle = glGetAttribLocation(program, "vPosition")
        val colorHandle = glGetUniformLocation(program, "uColor")
        val mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")

        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, vertexBuffer)

        glUniform4fv(colorHandle, 1, color, 0)
        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, ByteBuffer.allocateDirect(drawOrder.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply {
            put(drawOrder)
            position(0)
        })

        glDisableVertexAttribArray(positionHandle)
    }

    fun randomizeColor() {
        color = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
    }

    fun resetColor() {
        color = baseColor.copyOf()
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also {
            glShaderSource(it, shaderCode)
            glCompileShader(it)
        }
    }

    // Функции для нахождения минимальных и максимальных координат по каждой оси
    fun getMin(axis: Int): Float {
        return when (axis) {
            0 -> x - _cubeSize
            1 -> y - _cubeSize
            2 -> z - _cubeSize
            else -> throw IllegalArgumentException("Invalid axis")
        }
    }

    fun getMax(axis: Int): Float {
        return when (axis) {
            0 -> x + _cubeSize
            1 -> y + _cubeSize
            2 -> z + _cubeSize
            else -> throw IllegalArgumentException("Invalid axis")
        }
    }

    fun intersectRayWithCube(rayOrigin: Vector3, rayDir: Vector3): Float? {
        val tMin = FloatArray(3)
        val tMax = FloatArray(3)

        for (i in 0..2) {
            val invD = 1.0f / rayDir[i]
            var t0 = (getMin(i) - rayOrigin[i]) * invD
            var t1 = (getMax(i) - rayOrigin[i]) * invD
            if (invD < 0.0f) {
                val tmp = t0
                t0 = t1
                t1 = tmp
            }
            tMin[i] = t0
            tMax[i] = t1
        }

        val tEnter = maxOf(tMin[0], tMin[1], tMin[2])
        val tExit = minOf(tMax[0], tMax[1], tMax[2])

        return if (tEnter <= tExit && tExit >= 0f) tEnter else null
    }

}
