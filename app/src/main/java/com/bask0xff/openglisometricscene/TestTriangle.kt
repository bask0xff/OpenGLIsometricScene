package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class TestTriangle {
    private val vertexBuffer: FloatBuffer
    private val program: Int
    private val POSITION_COUNT = 3

    // Координаты треугольника (x, y, z)
    private val coords = floatArrayOf(
        0f, 0.5f, 0f,   // Верхняя вершина
        -0.5f, -0.5f, 0f, // Левая нижняя
        0.5f, -0.5f, 0f   // Правая нижняя
    )

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec4 vPosition;
        void main() {
            gl_Position = uMVPMatrix * vPosition;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        void main() {
            gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0); // Белый цвет
        }
    """.trimIndent()

    private var positionHandle = 0
    private var mvpMatrixHandle = 0

    init {
        // Инициализация буфера координат
        vertexBuffer = ByteBuffer.allocateDirect(coords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(coords)
                position(0)
            }

        // Компиляция шейдеров и линковка программы
        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = glCreateProgram().also {
            glAttachShader(it, vertexShader)
            glAttachShader(it, fragmentShader)
            glLinkProgram(it)
        }
    }

    fun draw(mvpMatrix: FloatArray) {
        glUseProgram(program)

        // Получаем handle атрибута позиции
        positionHandle = glGetAttribLocation(program, "vPosition")
        glEnableVertexAttribArray(positionHandle)
        glVertexAttribPointer(
            positionHandle,
            POSITION_COUNT,
            GL_FLOAT,
            false,
            POSITION_COUNT * 4,
            vertexBuffer
        )

        // Передаём матрицу трансформации
        mvpMatrixHandle = glGetUniformLocation(program, "uMVPMatrix")
        glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Рисуем треугольник
        glDrawArrays(GL_TRIANGLES, 0, 3)

        // Отключаем массив вершин
        glDisableVertexAttribArray(positionHandle)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also { shader ->
            glShaderSource(shader, shaderCode)
            glCompileShader(shader)

            val compiled = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val error = glGetShaderInfoLog(shader)
                glDeleteShader(shader)
                throw RuntimeException("Ошибка компиляции шейдера: $error")
            }
        }
    }
}
