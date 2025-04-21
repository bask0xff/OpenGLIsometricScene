package com.bask0xff.openglisometricscene

import android.opengl.EGLConfig
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.opengles.GL10

class IsoGLRenderer : GLSurfaceView.Renderer {
    private val cubes = mutableListOf<Cube>()

    override fun onSurfaceCreated(gl: GL10?, p1: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        for (x in 0 until 5) {
            for (z in 0 until 5) {
                if ((x + z) % 2 == 0) {
                    cubes.add(Cube(x.toFloat(), 0f, z.toFloat()))
                }
            }
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val viewMatrix = FloatArray(16)
        val projMatrix = FloatArray(16)
        val vpMatrix = FloatArray(16)

        Matrix.setLookAtM(viewMatrix, 0,
            5f, 8f, 10f,
            2.5f, 0f, 2.5f,
            0f, 1f, 0f)

        Matrix.frustumM(projMatrix, 0, -1f, 1f, -1f, 1f, 3f, 20f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        for (cube in cubes) {
            cube.draw(vpMatrix)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}
