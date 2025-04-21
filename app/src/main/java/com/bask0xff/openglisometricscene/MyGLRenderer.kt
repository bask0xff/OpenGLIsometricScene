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


    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

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
        val worldX = (x / 500) - 1 // Замените на подходящую логику
        val worldY = (y / 500) - 1 // Замените на подходящую логику
        Log.d(TAG, "handleTouch: ($x -> $worldX), ($y -> $worldY)")
        // Проверяем, был ли клик по одному из кубов
        for (cube in cubes) {
            if (worldX >= cube.x - 0.5f && worldX <= cube.x + 0.5f &&
                worldY >= cube.y - 0.5f && worldY <= cube.y + 0.5f) {
                // Если клик был внутри куба, меняем его цвет
                cube.randomizeColor()
            }
        }
    }
}

