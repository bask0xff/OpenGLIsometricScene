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

    private val cubeCoords = floatArrayOf(
        -0.5f, 0.5f, 0.5f,  -0.5f, -0.5f, 0.5f,   0.5f, -0.5f, 0.5f,   0.5f, 0.5f, 0.5f,
        -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f,  0.5f, -0.5f, -0.5f,  0.5f, 0.5f, -0.5f
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
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
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

    // Метод для пересечения луча с кубом (AABB)
    fun intersectRayWithCube(rayOrigin: Vector3, rayDir: Vector3): Boolean {
        var tMin = 0.0f
        var tMax = Float.MAX_VALUE

        for (i in 0 until 3) {
            val origin = rayOrigin[i]
            val direction = rayDir[i]

            val minBound = getMin(i)  // Минимальная координата по оси
            val maxBound = getMax(i)  // Максимальная координата по оси

            var t1 = (minBound - origin) / direction
            var t2 = (maxBound - origin) / direction

            if (t1 > t2) {
                val temp = t1
                t1 = t2
                t2 = temp
            }

            tMin = Math.max(tMin, t1)
            tMax = Math.min(tMax, t2)

            if (tMin > tMax) {
                return false // Луч не пересекает куб
            }
        }

        return true // Луч пересекает куб
    }

    // Получение минимальной координаты по оси
    private fun getMin(axis: Int): Float {
        return when (axis) {
            0 -> x - 0.5f
            1 -> y - 0.5f
            2 -> z - 0.5f
            else -> throw IndexOutOfBoundsException("Invalid axis")
        }
    }

    // Получение максимальной координаты по оси
    private fun getMax(axis: Int): Float {
        return when (axis) {
            0 -> x + 0.5f
            1 -> y + 0.5f
            2 -> z + 0.5f
            else -> throw IndexOutOfBoundsException("Invalid axis")
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also {
            glShaderSource(it, shaderCode)
            glCompileShader(it)
        }
    }
}
