package com.bask0xff.openglisometricscene.utils

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import android.opengl.GLES20.*
import android.util.Log
import java.nio.ShortBuffer

object GLUtils {
    /*fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(coords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer()
        fb.put(coords)
        fb.position(0)
        return fb
    }*/

    // Функция для создания FloatBuffer
    fun createFloatBuffer(data: FloatArray): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()
        floatBuffer.put(data)
        floatBuffer.position(0)
        return floatBuffer
    }

    // Функция для создания ShortBuffer
    fun createShortBuffer(data: ShortArray): ShortBuffer {
        val buffer = ByteBuffer.allocateDirect(data.size * 2)
        buffer.order(ByteOrder.nativeOrder())
        val shortBuffer = buffer.asShortBuffer()
        shortBuffer.put(data)
        shortBuffer.position(0)
        return shortBuffer
    }

    fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
        // Компилируем шейдеры
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Создаём программу
        val program = glCreateProgram()
        if (program == 0) {
            Log.e("GLUtils", "Error creating program")
            return 0
        }

        // Привязываем шейдеры к программе
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)

        // Линкуем программу
        glLinkProgram(program)

        // Проверяем успешность линковки
        val linkStatus = IntArray(1)
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("GLUtils", "Error linking program")
            glDeleteProgram(program)
            return 0
        }

        // Возвращаем ID программы
        return program
    }

    // Функция для загрузки и компиляции шейдера
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = glCreateShader(type)
        if (shader == 0) {
            Log.e("GLUtils", "Error creating shader of type $type")
            return 0
        }

        glShaderSource(shader, shaderCode)
        glCompileShader(shader)

        // Проверяем успешность компиляции
        val compileStatus = IntArray(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e("GLUtils", "Error compiling shader: ${getShaderLog(shader)}")
            glDeleteShader(shader)
            return 0
        }

        return shader
    }

    // Получаем лог компиляции шейдера
    private fun getShaderLog(shader: Int): String {
        val logLength = IntArray(1)
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, logLength, 0)
        val log = ByteArray(logLength[0])
        glGetShaderInfoLog(shader)
        return String(log)
    }
}
