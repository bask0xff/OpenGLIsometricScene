package com.bask0xff.openglisometricscene

import android.opengl.EGLConfig
import android.opengl.GLES10.GL_COLOR_BUFFER_BIT
import android.opengl.GLES10.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES10.glClear
import android.opengl.GLES10.glClearColor
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.opengles.GL10

class IsoGLRenderer : GLSurfaceView.Renderer {
    private val cubes = mutableListOf<Cube>()
    private val projectionMatrix = FloatArray(16)

    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var surfaceWidth = 1
    var surfaceHeight = 1

    override fun onSurfaceCreated(gl: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        Matrix.setIdentityM(projectionMatrix, 0)  // Инициализация проекционной матрицы

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
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        for (cube in cubes) {
            cube.draw(vpMatrix)
        }
    }

    //override fun onSurfaceDestroyed(gl: GL10?) {}

    fun handleTouch(screenX: Float, screenY: Float) {
        // Нормализуем координаты экрана в [-1, 1]
        val normalizedX = (2f * screenX) / surfaceWidth - 1f
        val normalizedY = 1f - (2f * screenY) / surfaceHeight

        // Преобразуем в мировые координаты через обратную матрицу проекции-вида
        val invertedVPMatrix = FloatArray(16)
        Matrix.invertM(invertedVPMatrix, 0, vpMatrix, 0)

        // Z = 0 (на плоскости XY)
        val nearPoint = floatArrayOf(normalizedX, normalizedY, 0f, 1f)
        val worldPoint = FloatArray(4)
        Matrix.multiplyMV(worldPoint, 0, invertedVPMatrix, 0, nearPoint, 0)

        val worldX = worldPoint[0] / worldPoint[3]
        val worldY = worldPoint[1] / worldPoint[3]

        Log.d("Touch", "World coords: x=$worldX, y=$worldY")

        for (cube in cubes) {
            if (worldX >= cube.x - 0.5f && worldX <= cube.x + 0.5f &&
                worldY >= cube.y - 0.5f && worldY <= cube.y + 0.5f) {
                cube.randomizeColor()
            }
        }
    }

}
