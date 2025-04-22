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
        // Генерируем вершины для шара (геодезическая сетка или просто треугольники)
        val numSlices = 20
        val numStacks = 20
        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

        for (i in 0..numStacks) {
            val phi = Math.PI * i / numStacks
            val z = radius * cos(phi.toFloat())
            val r = radius * sin(phi.toFloat())

            for (j in 0..numSlices) {
                val theta = 2.0 * Math.PI * j / numSlices
                val x = r * cos(theta.toFloat())
                val y = r * sin(theta.toFloat())
                vertexList.add(x)
                vertexList.add(y)
                vertexList.add(z)
            }
        }

        // Создание индексов для треугольников
        for (i in 0 until numStacks) {
            for (j in 0 until numSlices) {
                val first = (i * (numSlices + 1)) + j
                val second = first + numSlices + 1
                indexList.add(first.toShort())
                indexList.add(second.toShort())
                indexList.add((first + 1).toShort())

                indexList.add(second.toShort())
                indexList.add((second + 1).toShort())
                indexList.add((first + 1).toShort())
            }
        }

        // Преобразуем список в массивы
        vertices = vertexList.toFloatArray()
        indices = indexList.toShortArray()

        // Загружаем шейдеры и создаём программу
        program = GLUtils.createProgram(vertexShaderCode, fragmentShaderCode)
    }

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        glUseProgram(program)

        // Получаем атрибуты и униформы
        positionHandle = glGetAttribLocation(program, "vPosition")
        mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")
        colorHandle = glGetUniformLocation(program, "uColor")

        // Устанавливаем значения униформ
        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        glUniform4fv(colorHandle, 1, color, 0)

        // Создаем буфер для вершин
        val vertexBuffer = GLUtils.createFloatBuffer(vertices)
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, vertexBuffer)
        glEnableVertexAttribArray(positionHandle)

        // Создаем буфер для индексов
        val indexBuffer = GLUtils.createShortBuffer(indices)

        // Отрисовываем объект
        glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_SHORT, indexBuffer)

        // Отключаем атрибуты
        glDisableVertexAttribArray(positionHandle)
    }
}
