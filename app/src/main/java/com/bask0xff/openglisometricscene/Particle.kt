package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

class Particle(
    x: Float,
    y: Float,
    z: Float,
    color: FloatArray,
    sizeScale: Float,
    val velocity: Vector3,
    var lifeTime: Float
) {
    private val TAG = "Particle"
    private var x: Float = x
    private var y: Float = y
    private var z: Float = z
    private var color: FloatArray = color
    private var initialSizeScale = sizeScale
    private var currentSizeScale = sizeScale
    private var timeLived = 0f
    private var program: Int = 0
    private var vertexBuffer: FloatBuffer
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private var initialized = false

    init {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)

        // Инициализация буфера для одной точки
        val pointVertex = floatArrayOf(
            0f, 0f, 0f, color[0], color[1], color[2], color[3]
        )
        vertexBuffer = ByteBuffer.allocateDirect(pointVertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(pointVertex)
        vertexBuffer.position(0)
    }

    fun initialize() {
        if (initialized) return
        initialized = true

        val vertexShader = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec4 vColor;
            varying vec4 fragColor;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                gl_PointSize = 10.0;
                fragColor = vColor;
            }
        """.trimIndent()

        val fragmentShader = """
            precision mediump float;
            varying vec4 fragColor;
            void main() {
                gl_FragColor = fragColor;
            }
        """.trimIndent()

        val vShader = loadShader(GL_VERTEX_SHADER, vertexShader)
        val fShader = loadShader(GL_FRAGMENT_SHADER, fragmentShader)

        program = glCreateProgram().also {
            glAttachShader(it, vShader)
            glAttachShader(it, fShader)
            glLinkProgram(it)

            val linkStatus = IntArray(1)
            glGetProgramiv(it, GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error linking program: ${glGetProgramInfoLog(it)}")
                glDeleteProgram(it)
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also { shader ->
            glShaderSource(shader, shaderCode)
            glCompileShader(shader)
            val compileStatus = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: ${glGetShaderInfoLog(shader)}")
                glDeleteShader(shader)
            }
        }
    }

    fun update(deltaTime: Float): Boolean {
        timeLived += deltaTime
        Log.d(TAG, "update: Particle at ($x, $y, $z), timeLived=$timeLived, lifeTime=$lifeTime, sizeScale=$currentSizeScale")
        if (timeLived >= lifeTime) {
            Log.d(TAG, "update: Particle expired")
            return false
        }

        x += velocity.x * deltaTime
        y += velocity.y * deltaTime
        z += velocity.z * deltaTime

        currentSizeScale = initialSizeScale * (1f - timeLived / lifeTime)

        return true
    }

    fun draw(vpMatrix: FloatArray) {
        initialize()
        //Log.d(TAG, "draw: Rendering particle at ($x, $y, $z) with sizeScale=$currentSizeScale")

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        glUseProgram(program)

        val positionHandle = glGetAttribLocation(program, "vPosition")
        val colorHandle = glGetAttribLocation(program, "vColor")
        val mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")

        glEnable(GL_DEPTH_TEST)
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 7 * 4, vertexBuffer)

        glEnableVertexAttribArray(colorHandle)
        vertexBuffer.position(3)
        glVertexAttribPointer(colorHandle, 4, GL_FLOAT, false, 7 * 4, vertexBuffer)
        vertexBuffer.position(0)

        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        glDrawArrays(GL_POINTS, 0, 1)

        glDisableVertexAttribArray(positionHandle)
        glDisableVertexAttribArray(colorHandle)
    }
}