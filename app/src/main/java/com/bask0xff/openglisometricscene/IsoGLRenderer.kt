package com.bask0xff.openglisometricscene

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class IsoGLRenderer : GLSurfaceView.Renderer {
    private val cubes = mutableListOf<Cube>()
    val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var surfaceWidth = 1
    var surfaceHeight = 1

    private val nearPointNdc = FloatArray(4)
    private val farPointNdc = FloatArray(4)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(0.2f, 0.2f, 0.2f, 1f)
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
        glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height

        val ratio = width.toFloat() / height
        Matrix.orthoM(projectionMatrix, 0, -ratio * 5, ratio * 5, -5f, 5f, -10f, 10f)

        Matrix.setLookAtM(viewMatrix, 0,
            5f, 5f, 5f,
            0f, 0f, 0f,
            0f, 0f, 1f
        )

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        for (cube in cubes) {
            cube.draw(vpMatrix)
        }
    }

    // Function to handle touch and find closest cube
    fun handleTouch(
        x: Float,
        y: Float,
        screenWidth: Int,
        screenHeight: Int
    ): Boolean {
        val (rayOrigin, rayDir) = getRayOriginAndDirection(x, y, screenWidth, screenHeight, viewMatrix, projectionMatrix)

        var closestCube: Cube? = null
        var minDistance = Float.MAX_VALUE

        for (cube in cubes) {
            val distance = cube.intersectRayWithCube(rayOrigin, rayDir)
            if (distance != null && distance < minDistance) {
                minDistance = distance
                closestCube = cube
            }
        }

        closestCube?.randomizeColor()
        return closestCube != null
    }


    // Convert touch to world ray
    private fun getRayOriginAndDirection(
        x: Float,
        y: Float,
        screenWidth: Int,
        screenHeight: Int,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): Pair<Vector3, Vector3> {
        val normalizedX = (2.0f * x) / screenWidth - 1.0f
        val normalizedY = 1.0f - (2.0f * y) / screenHeight

        val nearPointNDC = floatArrayOf(normalizedX, normalizedY, -1.0f, 1.0f)
        val farPointNDC = floatArrayOf(normalizedX, normalizedY, 1.0f, 1.0f)

        val invertedMatrix = FloatArray(16)
        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invertedMatrix, 0, vpMatrix, 0)

        val nearPointWorld = FloatArray(4)
        val farPointWorld = FloatArray(4)

        Matrix.multiplyMV(nearPointWorld, 0, invertedMatrix, 0, nearPointNDC, 0)
        Matrix.multiplyMV(farPointWorld, 0, invertedMatrix, 0, farPointNDC, 0)

        for (i in 0..3) {
            nearPointWorld[i] /= nearPointWorld[3]
            farPointWorld[i] /= farPointWorld[3]
        }

        val rayOrigin = Vector3(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2])
        val rayEnd = Vector3(farPointWorld[0], farPointWorld[1], farPointWorld[2])
        val rayDirection = (rayEnd - rayOrigin).normalize()

        return rayOrigin to rayDirection
    }


    fun getCubes(): List<Cube> = cubes

    private fun intersectsCube(rayOrigin: FloatArray, rayDir: FloatArray, cube: Cube): Boolean {
        val min = floatArrayOf(cube.x - 0.5f, cube.y - 0.5f, cube.z - 0.5f)
        val max = floatArrayOf(cube.x + 0.5f, cube.y + 0.5f, cube.z + 0.5f)

        var tmin = (min[0] - rayOrigin[0]) / rayDir[0]
        var tmax = (max[0] - rayOrigin[0]) / rayDir[0]

        if (tmin > tmax) tmin = tmax.also { tmax = tmin }

        for (i in 1..2) {
            var t1 = (min[i] - rayOrigin[i]) / rayDir[i]
            var t2 = (max[i] - rayOrigin[i]) / rayDir[i]

            if (t1 > t2) t1 = t2.also { t2 = t1 }

            if (t1 > tmin) tmin = t1
            if (t2 < tmax) tmax = t2
            if (tmin > tmax) return false
        }

        return tmax > 0
    }
}
