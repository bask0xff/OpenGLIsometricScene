package com.bask0xff.openglisometricscene.ui.theme

import android.opengl.GLES20.*
import android.util.Log
import com.bask0xff.openglisometricscene.utils.GLUtils
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class Ball(private val radius: Float) {
    private val TAG = "Ball"
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0

    private val vertexShaderCode = """
        attribute vec4 vPosition;
        uniform mat4 uMVPMatrix;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 uColor;
        void main() {
            gl_FragColor = uColor;
        }
    """

    private val vertices: FloatArray
    private val indices: ShortArray

    init {
        val numSlices = 40
        val numStacks = 40

        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        // Генерация вершин
        for (i in 0..numStacks) {
            val phi = Math.PI * i / numStacks
            val z = radius * cos(phi).toFloat()
            val r = radius * sin(phi).toFloat()

            for (j in 0 until numSlices) { // <= важно: до numSlices, без включения последней точки
                val theta = 2.0 * Math.PI * j / numSlices
                val x = r * cos(theta).toFloat()
                val y = r * sin(theta).toFloat()

                vertexList.add(x)
                vertexList.add(y)
                vertexList.add(z)
            }
        }

        // Генерация индексов
        for (i in 0 until numStacks) {
            for (j in 0 until numSlices) {
                val first = i * numSlices + j
                val second = first + numSlices

                val next = (j + 1) % numSlices

                indexList.add(first.toShort())
                indexList.add((second + next - j).toShort())
                indexList.add((first + next - j).toShort())

                indexList.add((second + next - j).toShort())
                indexList.add((second).toShort())
                indexList.add((first + next - j).toShort())
            }
        }

        vertices = vertexList.toFloatArray()
        indices = indexList.toShortArray()

        // Создание программы OpenGL
        program = GLUtils.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        glEnable(GL_DEPTH_TEST)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glUseProgram(program)

        positionHandle = glGetAttribLocation(program, "vPosition")
        mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")
        colorHandle = glGetUniformLocation(program, "uColor")

        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        glUniform4fv(colorHandle, 1, color, 0)

        val vertexBuffer = GLUtils.createFloatBuffer(vertices)
        val indexBuffer = GLUtils.createShortBuffer(indices)

        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, vertexBuffer)
        glEnableVertexAttribArray(positionHandle)

        glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_SHORT, indexBuffer)

        glDisableVertexAttribArray(positionHandle)
    }
}
