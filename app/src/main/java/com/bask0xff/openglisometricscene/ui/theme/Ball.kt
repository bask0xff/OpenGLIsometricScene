package com.bask0xff.openglisometricscene.ui.theme

import android.opengl.GLES20.*
import com.bask0xff.openglisometricscene.utils.GLUtils
import javax.microedition.khronos.opengles.GL10

class Ball(private val radius: Float) {
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

    init {
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

        // Вставка кода для отрисовки мяча
        // Можете использовать треугольники или другой способ для отрисовки шара
    }
}
