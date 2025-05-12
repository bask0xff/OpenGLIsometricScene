package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class HexPrism(var x: Float, var y: Float, var z: Float, private val baseColor: FloatArray, private val sizeScale: Float = 1.0f) {

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

    private val _prismRadius = 0.42f * sizeScale // Radius of the hexagonal base
    private val prismHeight = 0.3f * sizeScale / 3f // Height of the prism along Z-axis (3x shorter than original)

    fun prismSize(): Float {
        return _prismRadius
    }

    // Define vertices for a hexagonal prism
    private val hexVertices: FloatArray

    init {
        // Generate vertices for top and bottom hexagonal faces
        val vertices = mutableListOf<Float>()
        val numSides = 6
        val topZ = prismHeight
        val bottomZ = -prismHeight

        // Top face vertices (6 vertices)
        for (i in 0 until numSides) {
            val angle = 2.0 * Math.PI * i / numSides
            val vx = _prismRadius * cos(angle).toFloat()
            val vy = _prismRadius * sin(angle).toFloat()
            vertices.addAll(listOf(vx, vy, topZ, color[0], color[1], color[2], color[3]))
        }

        // Bottom face vertices (6 vertices)
        for (i in 0 until numSides) {
            val angle = 2.0 * Math.PI * i / numSides
            val vx = _prismRadius * cos(angle).toFloat()
            val vy = _prismRadius * sin(angle).toFloat()
            vertices.addAll(listOf(vx, vy, bottomZ, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3]))
        }

        // Side face vertices (duplicate vertices for distinct coloring)
        for (i in 0 until numSides) {
            val angle = 2.0 * Math.PI * i / numSides
            val nextAngle = 2.0 * Math.PI * ((i + 1) % numSides) / numSides
            val vx1 = _prismRadius * cos(angle).toFloat()
            val vy1 = _prismRadius * sin(angle).toFloat()
            val vx2 = _prismRadius * cos(nextAngle).toFloat()
            val vy2 = _prismRadius * sin(nextAngle).toFloat()
            // Side face i (4 vertices: top1, top2, bottom2, bottom1)
            vertices.addAll(listOf(
                vx1, vy1, topZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
                vx2, vy2, topZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
                vx2, vy2, bottomZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
                vx1, vy1, bottomZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3]
            ))
        }

        hexVertices = vertices.toFloatArray()

        // Define indices for drawing
        val indices = mutableListOf<Short>()
        // Top face (two triangles per quad)
        for (i in 1 until numSides - 1) {
            indices.addAll(listOf(0, i.toShort(), (i + 1).toShort()))
        }
        // Bottom face (vertices 6 to 11)
        for (i in 1 until numSides - 1) {
            indices.addAll(listOf(6, (6 + i).toShort(), (6 + i + 1).toShort()))
        }
        // Side faces (6 quads, each with 2 triangles)
        val sideBase = numSides * 2
        for (i in 0 until numSides) {
            val base = sideBase + i * 4
            indices.addAll(listOf(
                base.toShort(), (base + 1).toShort(), (base + 2).toShort(),
                base.toShort(), (base + 2).toShort(), (base + 3).toShort()
            ))
        }

        val bb = ByteBuffer.allocateDirect(hexVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(hexVertices)
        vertexBuffer.position(0)

        val ib = ByteBuffer.allocateDirect(indices.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer.put(indices.toShortArray())
        indexBuffer.position(0)

        // Shaders
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
                vec4 glowColor = vec4(1.0, 1.0, 0.0, 1.0); // Yellow glow
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

    fun getPrismColor(): FloatArray {
        return color
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

        glDrawElements(GL_TRIANGLES, indexBuffer.capacity(), GL_UNSIGNED_SHORT, indexBuffer)

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
        val vertices = mutableListOf<Float>()
        val numSides = 6
        val topZ = prismHeight
        val bottomZ = -prismHeight

        // Top face
        for (i in 0 until numSides) {
            val angle = 2.0 * Math.PI * i / numSides
            val vx = _prismRadius * cos(angle).toFloat()
            val vy = _prismRadius * sin(angle).toFloat()
            vertices.addAll(listOf(vx, vy, topZ, color[0], color[1], color[2], color[3]))
        }

        // Bottom face
        for (i in 0 until numSides) {
            val angle = 2.0 * Math.PI * i / numSides
            val vx = _prismRadius * cos(angle).toFloat()
            val vy = _prismRadius * sin(angle).toFloat()
            vertices.addAll(listOf(vx, vy, bottomZ, color[0] * 0.5f, color[1] * 0.5f, color[2] * 0.5f, color[3]))
        }

        // Side faces
        for (i in 0 until numSides) {
            val angle = 2.0 * Math.PI * i / numSides
            val nextAngle = 2.0 * Math.PI * ((i + 1) % numSides) / numSides
            val vx1 = _prismRadius * cos(angle).toFloat()
            val vy1 = _prismRadius * sin(angle).toFloat()
            val vx2 = _prismRadius * cos(nextAngle).toFloat()
            val vy2 = _prismRadius * sin(nextAngle).toFloat()
            vertices.addAll(listOf(
                vx1, vy1, topZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
                vx2, vy2, topZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
                vx2, vy2, bottomZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3],
                vx1, vy1, bottomZ, color[0] * 0.67f, color[1] * 0.67f, color[2] * 0.67f, color[3]
            ))
        }

        vertexBuffer.clear()
        vertexBuffer.put(vertices.toFloatArray())
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
            0 -> x - _prismRadius
            1 -> y - _prismRadius
            2 -> z - prismHeight
            else -> throw IllegalArgumentException("Invalid axis")
        }
    }

    fun getMax(axis: Int): Float {
        return when (axis) {
            0 -> x + _prismRadius
            1 -> y + _prismRadius
            2 -> z + prismHeight
            else -> throw IllegalArgumentException("Invalid axis")
        }
    }

    fun intersectRayWithHexPrism(rayOrigin: Vector3, rayDir: Vector3): Float? {
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
        if (!isSelected) {
            isFalling = true
            fallSpeed = 5f
        }
    }
}