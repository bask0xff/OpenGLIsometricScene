package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {

    private val cubes = mutableListOf<Cube>()

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 1f)
        glEnable(GL_DEPTH_TEST)

        for (x in 0..4) {
            for (y in 0..4) {
                cubes.add(Cube(x.toFloat(), y.toFloat(), 0f))
            }
        }
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 10f)

        // Позиция камеры для изометрии
        Matrix.setLookAtM(viewMatrix, 0,
            5f, 7f, 5f,  // камера
            2f, 0f, 2f,  // центр
            0f, 1f, 0f   // вверх
        )
    }

    override fun onDrawFrame(unused: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        for (cube in cubes) {
            cube.draw(vpMatrix)
        }
    }
}

