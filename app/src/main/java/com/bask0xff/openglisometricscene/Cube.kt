package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.random.Random

class Cube(var x: Float, var y: Float, var z: Float, private val baseColor: FloatArray, private val sizeScale: Float = 1.0f) {

    private var color: FloatArray = baseColor.copyOf()
    private var isSelected: Boolean = false
    var isFalling: Boolean = false
    var targetZ: Float = z
    var startZ: Float = z
    var fallProgress: Float = 0f
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val program: Int

    private val _cubeSize = 0.3f * sizeScale // Размер куба с учётом масштаба
    private val cubeHeight = 0.3f * sizeScale

    fun cubeSize(): Float {
        return _cubeSize
    }

    private val cubeVertices = floatArrayOf(
        // Top face vertices (z = cubeHeight)
        -_cubeSize, _cubeSize, cubeHeight,  color[0], color[1], color[2], color[3], // 0
        -_cubeSize, -_cubeSize, cubeHeight, color[0], color[1], color[2], color[3], // 1
        _cubeSize, -_cubeSize, cubeHeight,  color[0], color[1], color[2], color[3], // 2
        _cubeSize, _cubeSize, cubeHeight,   color[0], color[1], color[2], color[3], // 3
        // Bottom face vertices (z = -cubeHeight)
        -_cubeSize, _cubeSize, -cubeHeight, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3], // 4
        -_cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3], // 5
        _cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3], // 6
        _cubeSize, _cubeSize, -cubeHeight,  color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3], // 7
        // Left face additional vertices (x = -_cubeSize)
        -_cubeSize, _cubeSize, cubeHeight,  color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3], // 8
        -_cubeSize, -_cubeSize, cubeHeight, color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3], // 9
        -_cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3], // 10
        -_cubeSize, _cubeSize, -cubeHeight, color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3], // 11
        // Right face additional vertices (x = _cubeSize)
        _cubeSize, _cubeSize, cubeHeight,   color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3], // 12
        _cubeSize, -_cubeSize, cubeHeight,  color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3], // 13
        _cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3], // 14
        _cubeSize, _cubeSize, -cubeHeight,  color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3] // 15
    )

    private val drawOrder = shortArrayOf(
        // Top face
        0, 1, 2, 0, 2, 3,
        // Bottom face
        4, 5, 6, 4, 6, 7,
        // Left face
        8, 9, 10, 8, 10, 11,
        // Right face
        12, 14, 13, 12, 15, 14,
        // Front face
        1, 5, 6, 1, 6, 2,
        // Back face
        0, 4, 7, 0, 7, 3
    )

    init {
        val bb = ByteBuffer.allocateDirect(cubeVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(cubeVertices)
        vertexBuffer.position(0)

        val ib = ByteBuffer.allocateDirect(drawOrder.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer.put(drawOrder)
        indexBuffer.position(0)

        val vertexShader = loadShader(GL_VERTEX_SHADER, """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec4 vColor;
            varying vec4 fColor;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                fColor = vColor;
            }
        """)

        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, """
            precision mediump float;
            varying vec4 fColor;
            uniform int uIsSelected;
            void main() {
                vec4 glowColor = vec4(1.0, 1.0, 0.0, 1.0); // Жёлтый ореол
                vec4 baseColor = fColor;
                if (uIsSelected == 1) {
                    baseColor.rgb = baseColor.rgb * 1.5 + glowColor.rgb * 0.3;
                }
                gl_FragColor = baseColor;
            }
        """)

        program = glCreateProgram().also {
            glAttachShader(it, vertexShader)
            glAttachShader(it, fragmentShader)
            glLinkProgram(it)
        }
    }

    fun draw(vpMatrix: FloatArray) {
        val offset = 1f
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x * offset, y * offset, z * offset)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        glUseProgram(program)

        val positionHandle = glGetAttribLocation(program, "vPosition")
        val colorHandle = glGetAttribLocation(program, "vColor")
        val mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")
        val isSelectedHandle = glGetUniformLocation(program, "uIsSelected")

        glEnable(GL_DEPTH_TEST)
        glDisable(GL_CULL_FACE)

        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 7 * 4, vertexBuffer)

        glEnableVertexAttribArray(colorHandle)
        vertexBuffer.position(3)
        glVertexAttribPointer(colorHandle, 4, GL_FLOAT, false, 7 * 4, vertexBuffer)
        vertexBuffer.position(0)

        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        glUniform1i(isSelectedHandle, if (isSelected) 1 else 0)

        glDrawElements(GL_TRIANGLES, drawOrder.size, GL_UNSIGNED_SHORT, indexBuffer)

        glDisableVertexAttribArray(positionHandle)
        glDisableVertexAttribArray(colorHandle)
    }

    fun randomizeColor() {
        color = floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
        updateVertexColors()
    }

    fun resetColor() {
        color = baseColor.copyOf()
        updateVertexColors()
    }

    fun setSelected(selected: Boolean) {
        isSelected = selected
    }

    private fun updateVertexColors() {
        val updatedVertices = floatArrayOf(
            // Top face
            -_cubeSize, _cubeSize, cubeHeight,  color[0], color[1], color[2], color[3],
            -_cubeSize, -_cubeSize, cubeHeight, color[0], color[1], color[2], color[3],
            _cubeSize, -_cubeSize, cubeHeight,  color[0], color[1], color[2], color[3],
            _cubeSize, _cubeSize, cubeHeight,   color[0], color[1], color[2], color[3],
            // Bottom face
            -_cubeSize, _cubeSize, -cubeHeight, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3],
            -_cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3],
            _cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3],
            _cubeSize, _cubeSize, -cubeHeight,  color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3],
            // Left face
            -_cubeSize, _cubeSize, cubeHeight,  color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3],
            -_cubeSize, -_cubeSize, cubeHeight, color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3],
            -_cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3],
            -_cubeSize, _cubeSize, -cubeHeight, color[0] * 0.34f, color[1] * 0.34f, color[2] * 0.34f, color[3],
            // Right face
            _cubeSize, _cubeSize, cubeHeight,   color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
            _cubeSize, -_cubeSize, cubeHeight,  color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
            _cubeSize, -_cubeSize, -cubeHeight, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
            _cubeSize, _cubeSize, -cubeHeight,  color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3]
        )
        vertexBuffer.clear()
        vertexBuffer.put(updatedVertices)
        vertexBuffer.position(0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also {
            glShaderSource(it, shaderCode)
            glCompileShader(it)
        }
    }

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

    private var fallSpeed = 0f
    private val gravity = -9.8f

    fun updateFall(deltaTime: Float) {
        if (!isFalling) return

        fallSpeed += gravity * deltaTime
        z += fallSpeed * deltaTime

        if (z <= targetZ) {
            z = targetZ
            isFalling = false
            fallSpeed = 0f
        }
    }

    fun startFalling() {
        if (!isFalling) {
            isFalling = true
            fallSpeed = 5f
        }
    }
}