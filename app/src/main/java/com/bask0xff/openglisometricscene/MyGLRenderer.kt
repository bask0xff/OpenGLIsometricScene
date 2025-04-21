package com.bask0xff.openglisometricscene

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {

    private val TAG = "MyGLRenderer"
    private val cubes = mutableListOf<Cube>()

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var surfaceWidth = 1
    var surfaceHeight = 1

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 1f)
        glEnable(GL_DEPTH_TEST)

        val colors = listOf(
            floatArrayOf(1f, 0f, 0f, 1f), // Red
            floatArrayOf(0f, 1f, 0f, 1f), // Green
            floatArrayOf(0f, 0f, 1f, 1f), // Blue
            floatArrayOf(1f, 1f, 0f, 1f), // Yellow
            floatArrayOf(1f, 0f, 1f, 1f), // Magenta
            floatArrayOf(0f, 1f, 1f, 1f), // Cyan
            floatArrayOf(1f, 0.5f, 0f, 1f), // Orange
            floatArrayOf(0.6f, 0f, 1f, 1f)  // Violet
        )

        for (x in 0..4) {
            for (y in 0..4) {
                val color = colors.random()
                cubes.add(Cube(x.toFloat(), y.toFloat(), 0f, color))
            }
        }
    }

    private fun updateProjectionMatrix(width: Int, height: Int) {
        val ratio = width.toFloat() / height
        // Простая ортографическая проекция (прямоугольная)
        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, -10f, 10f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        surfaceWidth = width
        surfaceHeight = height
        updateProjectionMatrix(width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        Matrix.setIdentityM(projectionMatrix, 0)
        Matrix.orthoM(
            projectionMatrix, 0,
            -ratio * 5, ratio * 5, // left, right
            -5f, 5f,               // bottom, top
            -10f, 10f              // near, far
        )

        // Камера в изометрической позиции: смотрит под углом сверху
        Matrix.setLookAtM(
            viewMatrix, 0,
            5f, 5f, 5f,     // eye
            0f, 0f, 0f,     // center
            0f, 1f, 0f      // up
        )

        // multiply projection and view to get final MVP matrix
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }


    override fun onDrawFrame(unused: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        for (cube in cubes) {
            cube.draw(vpMatrix)
        }
    }

    fun handleTouch(x: Float, y: Float) {
        // Преобразуем координаты экрана в мировые координаты
        //val worldX = (x / width.toFloat()) * 2f - 1f
        //val worldY = 1f - (y / height.toFloat()) * 2f
        val normalizedX = (x / surfaceWidth.toFloat()) * 2f - 1f
        val normalizedY = 1f - (y / surfaceHeight.toFloat()) * 2f


        Log.d(TAG, "handleTouch: ($x -> $normalizedX), ($y -> $normalizedY)")
        // Проверяем, был ли клик по одному из кубов
        for (cube in cubes) {
            // Проверяем, попадает ли клик в пределы куба
            if (normalizedX >= cube.x - 0.5f && normalizedX <= cube.x + 0.5f &&
                normalizedY >= cube.y - 0.5f && normalizedY <= cube.y + 0.5f) {
                // Если клик был внутри куба, меняем его цвет
                cube.randomizeColor()
                break  // Останавливаем цикл после первого совпадения
            }
        }
    }
}

