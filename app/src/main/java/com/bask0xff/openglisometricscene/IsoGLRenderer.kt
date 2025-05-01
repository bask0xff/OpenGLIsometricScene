package com.bask0xff.openglisometricscene

import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.bask0xff.openglisometricscene.ui.theme.Ball
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.random.Random
import kotlin.math.abs

class IsoGLRenderer : GLSurfaceView.Renderer {
    private val TAG = "IsoGLRenderer"
    private val cubes = mutableListOf<Cube>()
    private var smallCube: Cube? = null
    val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var surfaceWidth = 1
    var surfaceHeight = 1

    private val triangleCoords = floatArrayOf(
        0.0f, 1.0f, 0.0f, // Вершина 1
        -1.0f, -1.0f, 0.0f, // Вершина 2
        1.0f, -1.0f, 0.0f  // Вершина 3
    )
    private var positionHandle = 0
    private var uMVPMatrixHandle = 0

    private var rotationAngle: Float = 0f

    private var sphereCoords = FloatArray(1000) // Массив для шара

    private val mMVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)

    private val triangleBuffer: FloatBuffer
    private val sphereBuffer: FloatBuffer

    private var ball = Ball(10f)
    private lateinit var testTriangle: TestTriangle
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val cube = Cube(0f, 0f, 0f, floatArrayOf(1f, 0f, 0f, 1f))
    private var ballCube: Cube? = null
    private var selectedCube: Cube? = null
    private var lastFrameTime: Long = 0

    private val nearPointNdc = FloatArray(4)
    private val farPointNdc = FloatArray(4)

    var fieldSizeX = 5
    var fieldSizeY = 5
    var cubeSize = 0.5f

    private val heightMap = Array(10) { IntArray(10) }

    init {
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                heightMap[i][j] = (1..5).random()
            }
        }
        triangleBuffer = ByteBuffer.allocateDirect(triangleCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        triangleBuffer.put(triangleCoords).position(0)

        generateSphereCoordinates(1.0f)
        sphereBuffer = ByteBuffer.allocateDirect(sphereCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        sphereBuffer.put(sphereCoords).position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(0.2f, 0.2f, 0.2f, 1f)
        glEnable(GL_DEPTH_TEST)

        Matrix.setIdentityM(mMVPMatrix, 0)
        Matrix.setIdentityM(mProjectionMatrix, 0)
        Matrix.setIdentityM(mViewMatrix, 0)

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

        testTriangle = TestTriangle()

        synchronized(cubes) {
            val cells = 5
            cubes.clear()
            var cubeCount = 0
            for (x in 0 until cells) {
                for (y in 0 until cells) {
                    var height = Random.nextFloat() * 3f
                    for (z in 0..height.toInt()) {
                        val color = colors.random()
                        val offset = 0.60f
                        cubes.add(
                            Cube(
                                x.toFloat() * offset,
                                y.toFloat() * offset,
                                z.toFloat() * offset,
                                color
                            )
                        )
                        Log.d(TAG, "onSurfaceCreated: Added cube $cubeCount at (${x * offset}, ${y * offset}, ${z * offset})")
                        cubeCount++
                    }
                }
            }
            Log.d(TAG, "onSurfaceCreated: Total cubes created: ${cubes.size}")

            if (cubes.isNotEmpty()) {
                val randomCube = cubes.random()
                val smallCubeSize = 0.0f
                smallCube = Cube(
                    randomCube.x,
                    randomCube.y,
                    randomCube.z + randomCube.cubeSize() * 2,
                    floatArrayOf(1f, 1f, 1f, 1f),
                    smallCubeSize
                )
                Log.d(TAG, "onSurfaceCreated: Added small cube at (${smallCube?.x}, ${smallCube?.y}, ${smallCube?.z})")
            }
        }

        ball = Ball(1f)
        lastFrameTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height

        val ratio = width.toFloat() / height
        Matrix.orthoM(projectionMatrix, 0, -ratio * 5, ratio * 5, -5f, 5f, -10f, 10f)

        Matrix.setLookAtM(
            viewMatrix, 0,
            5f, 5f, 5f,
            0f, 0f, 0f,
            0f, 0f, 1f
        )

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Log.d(TAG, "onSurfaceChanged: Viewport set to $width x $height")
    }

    private fun drawTriangle() {
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, triangleBuffer)
        glEnableVertexAttribArray(positionHandle)
        glDrawArrays(GL_TRIANGLES, 0, 3)
    }

    private fun generateSphereCoordinates(radius: Float) {
        val sphereList = mutableListOf<Float>()
        val slices = 20
        val stacks = 20
        for (i in 0 until stacks) {
            val phi = Math.PI * (i / (stacks - 1).toDouble())
            for (j in 0 until slices) {
                val theta = 2.0 * Math.PI * (j / slices.toDouble())
                val x = (radius * Math.sin(phi) * Math.cos(theta)).toFloat()
                val y = (radius * Math.sin(phi) * Math.sin(theta)).toFloat()
                val z = (radius * Math.cos(phi)).toFloat()
                sphereList.add(x)
                sphereList.add(y)
                sphereList.add(z)
            }
        }
        sphereCoords = sphereList.toFloatArray()
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0f
        lastFrameTime = currentTime

        synchronized(cubes) {
            cubes.forEach { it.updateFall(deltaTime) }

            for (i in 0 until 10) {
                for (j in 0 until 10) {
                    val height = heightMap[i][j]
                    for (k in 0 until height) {
                        val modelMatrix = FloatArray(16)
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, i.toFloat(), k.toFloat(), j.toFloat())
                        val mvpMatrix = FloatArray(16)
                        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                        cube.draw(mvpMatrix)
                    }
                }
            }

            ballCube?.let {
                val basePosition = Vector3(it.x, it.y, it.z + 0.5f)
                val modelMatrix = FloatArray(16)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, basePosition.x, basePosition.y, basePosition.z)
                Matrix.scaleM(modelMatrix, 0, 3.0f, 3.0f, 3.0f)
                val mvpMatrix = FloatArray(16)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            }

            for (cube in cubes) {
                cube.draw(vpMatrix)
            }

            smallCube?.draw(vpMatrix)
        }

        Log.d(TAG, "onDrawFrame: Rendered ${cubes.size} cubes and small cube at (${smallCube?.x}, ${smallCube?.y}, ${smallCube?.z})")
    }

    fun handleTouch(x: Float, y: Float, screenWidth: Int, screenHeight: Int): Int {
        Log.d(TAG, "handleTouch: Touch at ($x, $y), screen size: $screenWidth x $screenHeight")
        val (rayOrigin, rayDir) = getRayOriginAndDirection(x, y, screenWidth, screenHeight, viewMatrix, projectionMatrix)
        Log.d(TAG, "handleTouch: Ray origin: (${rayOrigin.x}, ${rayOrigin.y}, ${rayOrigin.z})")
        Log.d(TAG, "handleTouch: Ray direction: (${rayDir.x}, ${rayDir.y}, ${rayDir.z})")

        var closestIndex = -1
        var minDistance = Float.MAX_VALUE

        synchronized(cubes) {
            if (cubes.isEmpty()) {
                Log.d(TAG, "handleTouch: No cubes available")
                return closestIndex
            }

            cubes.forEachIndexed { index, cube ->
                val distance = cube.intersectRayWithCube(rayOrigin, rayDir)
                Log.d(TAG, "handleTouch: Cube $index at (${cube.x}, ${cube.y}, ${cube.z}), distance: ${distance ?: "null"}")
                if (distance != null && distance < minDistance) {
                    minDistance = distance
                    closestIndex = index
                    Log.d(TAG, "handleTouch: New closest cube index: $index at distance $distance")
                }
            }

            if (closestIndex >= 0) {
                val selectedCubeLocal = cubes[closestIndex]
                selectedCube?.setSelected(false)
                selectedCubeLocal.setSelected(true)
                selectedCubeLocal.randomizeColor()
                ballCube = selectedCubeLocal
                Log.d(TAG, "handleTouch: Selected cube index: $closestIndex at (${selectedCubeLocal.x}, ${selectedCubeLocal.y}, ${selectedCubeLocal.z})")

                smallCube?.let {
                    it.x = selectedCubeLocal.x
                    it.y = selectedCubeLocal.y
                    it.z = selectedCubeLocal.z + selectedCubeLocal.cubeSize() * 2
                    Log.d(TAG, "handleTouch: Moved small cube to (${it.x}, ${it.y}, ${it.z})")
                }

                cubes.removeAt(closestIndex)
                Log.d(TAG, "handleTouch: Removed cube at index $closestIndex")

                val removedX = selectedCubeLocal.x
                val removedY = selectedCubeLocal.y
                val removedZ = selectedCubeLocal.z
                val offset = 0.60f
                val cubesAbove = cubes.filter {
                    abs(it.x - removedX) < 0.01f && abs(it.y - removedY) < 0.01f && it.z > removedZ
                }
                cubesAbove.forEach { cube ->
                    cube.isFalling = true
                    cube.startZ = cube.z
                    cube.targetZ = cube.z - offset
                    cube.fallProgress = 0f
                    Log.d(TAG, "handleTouch: Cube at (${cube.x}, ${cube.y}, ${cube.z}) will fall to z=${cube.targetZ}")
                }

                ballCube = null
                selectedCube = null
            } else {
                selectedCube?.setSelected(false)
                selectedCube = null
                Log.d(TAG, "handleTouch: No cube selected")
            }
        }

        Log.d(TAG, "handleTouch: Returning index: $closestIndex")
        return closestIndex
    }

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
        Log.d(TAG, "getRayOriginAndDirection: Normalized coords: ($normalizedX, $normalizedY)")

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
}