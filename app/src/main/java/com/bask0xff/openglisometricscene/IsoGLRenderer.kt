package com.bask0xff.openglisometricscene

import android.annotation.SuppressLint
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.random.Random

@SuppressLint("NewApi")
class IsoGLRenderer : GLSurfaceView.Renderer {
    private val TAG = "IsoGLRenderer"
    private val cubes = mutableListOf<Cube>()
    private val particles = mutableListOf<Particle>()
    private var smallCube: Cube? = null
    private var parentCube: Cube? = null
    val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    var surfaceWidth = 1
    var surfaceHeight = 1

    private var gridBuffer: FloatBuffer? = null
    private var gridVertexCount: Int = 0
    private var gridProgram: Int = 0
    private var gridPositionHandle: Int = 0
    private var gridMVPMatrixHandle: Int = 0

    private var selectedCube: Cube? = null
    private var lastFrameTime: Long = 0
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
        initializeGrid()
    }

    private fun initializeGrid() {
        val gridSizeX = fieldSizeX.toFloat()
        val gridSizeY = fieldSizeY.toFloat()
        val cellSize = 0.60f
        val gridCoords = mutableListOf<Float>()

        for (i in 0..fieldSizeX) {
            val x = i * cellSize
            gridCoords.addAll(listOf(
                x, 0f, 0f,
                x, gridSizeY * cellSize, 0f
            ))
        }

        for (j in 0..fieldSizeY) {
            val y = j * cellSize
            gridCoords.addAll(listOf(
                0f, y, 0f,
                gridSizeX * cellSize, y, 0f
            ))
        }

        gridVertexCount = gridCoords.size / 3

        gridBuffer = ByteBuffer.allocateDirect(gridCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        gridBuffer?.put(gridCoords.toFloatArray())?.position(0)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return glCreateShader(type).also { shader ->
            glShaderSource(shader, shaderCode)
            glCompileShader(shader)
            val compileStatus = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e(TAG, "Error compiling shader: ${glGetShaderInfoLog(shader)}")
                glDeleteShader(shader)
            }
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(0.2f, 0.2f, 0.2f, 1f)
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LESS)
        glDisable(GL_BLEND)
        makeCubes()
        lastFrameTime = System.nanoTime()

        // Инициализация кубов
        synchronized(cubes) {
            //cubes.forEach { it.initialize() }
            //smallCube?.initialize()
        }
    }

    private fun makeCubes() {
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

        val vertexShader = loadShader(GL_VERTEX_SHADER, """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent())

        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
            }
        """.trimIndent())

        gridProgram = glCreateProgram().also {
            glAttachShader(it, vertexShader)
            glAttachShader(it, fragmentShader)
            glLinkProgram(it)
            gridPositionHandle = glGetAttribLocation(it, "vPosition")
            gridMVPMatrixHandle = glGetUniformLocation(it, "uMVPMatrix")
        }

        synchronized(cubes) {
            val cells = 5
            cubes.clear()
            var cubeCount = 0
            for (x in 0 until cells) {
                for (y in 0 until cells) {
                    var height = Random.nextFloat() * 7f
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
                val smallCubeSize = 0.2f
                smallCube = Cube(
                    randomCube.x,
                    randomCube.y,
                    randomCube.z + randomCube.cubeSize(),
                    floatArrayOf(1f, 1f, 1f, 1f),
                    smallCubeSize
                )
                parentCube = randomCube
                Log.d(TAG, "onSurfaceCreated: Added small cube at (${smallCube?.x}, ${smallCube?.y}, ${smallCube?.z}) on parent cube at (${parentCube?.x}, ${parentCube?.y}, ${parentCube?.z})")
            }
        }
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

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glDisable(GL_BLEND)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        val currentTime = System.nanoTime()
        val deltaTime = minOf((currentTime - lastFrameTime) / 1_000_000_000.0f, 0.1f)
        lastFrameTime = currentTime

        if (cubes.size == 0) {
            Log.d(TAG, "onDrawFrame: NO CUBES !!!")
            makeCubes()
        }

        drawGrid()

        synchronized(cubes) {
            particles.removeAll { !it.update(deltaTime) }
            particles.forEach { it.draw(vpMatrix) }

            cubes.forEach { it.updateFall(deltaTime) }
            smallCube?.let { small ->
                parentCube?.let { parent ->
                    if (parent.isFalling) {
                        small.z = parent.z + parent.cubeSize()
                    }
                }
            }

            for (cube in cubes) {
                cube.draw(vpMatrix)
            }
            smallCube?.draw(vpMatrix)
        }
    }

    private fun drawGrid() {
        glUseProgram(gridProgram)
        glUniformMatrix4fv(gridMVPMatrixHandle, 1, false, vpMatrix, 0)
        gridBuffer?.let {
            it.position(0)
            glVertexAttribPointer(gridPositionHandle, 3, GL_FLOAT, false, 0, it)
            glEnableVertexAttribArray(gridPositionHandle)
        }
        glDepthMask(false)
        glDrawArrays(GL_LINES, 0, gridVertexCount)
        glDepthMask(true)
        glDisableVertexAttribArray(gridPositionHandle)
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
                //selectedCube?.selectCube(false)
                //selectedCubeLocal.selectCube(true)
                selectedCubeLocal.randomizeColor()
                selectedCube = selectedCubeLocal
                Log.d(TAG, "handleTouch: Selected cube index: $closestIndex at (${selectedCubeLocal.x}, ${selectedCubeLocal.y}, ${selectedCubeLocal.z})")

                smallCube?.let {
                    it.x = selectedCubeLocal.x
                    it.y = selectedCubeLocal.y
                    it.z = selectedCubeLocal.z + selectedCubeLocal.cubeSize()
                    parentCube = selectedCubeLocal
                    Log.d(TAG, "handleTouch: Moved small cube to (${it.x}, ${it.y}, ${it.z}) on parent cube at (${parentCube?.x}, ${parentCube?.y}, ${parentCube?.z})")
                }

                Log.d(TAG, "handleTouch: Creating 20 particles for explosion")
                repeat(20) {
                    val velocity = Vector3(
                        Random.nextFloat() * 4f - 2f,
                        Random.nextFloat() * 4f - 2f,
                        Random.nextFloat() * 4f - 2f
                    ).normalize() * 2f
                    particles.add(
                        Particle(
                            x = selectedCubeLocal.x,
                            y = selectedCubeLocal.y,
                            z = selectedCubeLocal.z,
                            color = selectedCubeLocal.getCubeColor(),
                            sizeScale = 0.2f,
                            velocity = velocity,
                            lifeTime = 3f
                        )
                    )
                }
                Log.d(TAG, "handleTouch: Created 20 particles at (${selectedCubeLocal.x}, ${selectedCubeLocal.y}, ${selectedCubeLocal.z}), particle count: ${particles.size}")

                cubes.removeAt(closestIndex)
                Log.d(TAG, "handleTouch: Removed cube at index $closestIndex")

                if (selectedCubeLocal == parentCube) {
                    parentCube = null
                    Log.d(TAG, "handleTouch: Parent cube removed, parentCube set to null")
                }

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

                selectedCube = null
            } else {
                //selectedCube?.selectCube(false)
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