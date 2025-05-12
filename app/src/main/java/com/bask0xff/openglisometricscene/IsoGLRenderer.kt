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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@SuppressLint("NewApi")
class IsoGLRenderer : GLSurfaceView.Renderer {
    private var selectedPrism: HexPrism? = null
    private val TAG = "IsoGLRenderer"
    private val hexPrisms = mutableListOf<HexPrism>()
    private val particles = mutableListOf<Particle>()
    private var smallHexPrism: HexPrism? = null
    private var parentHexPrism: HexPrism? = null
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

    private var selectedHexPrism: HexPrism? = null
    private var lastFrameTime: Long = 0
    var fieldSizeX = 5
    var fieldSizeY = 5
    var hexSize = 0.3f // Radius of the hexagon

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
        val gridCoords = mutableListOf<Float>()
        val numSides = 6
        val radius = hexSize // Match the prism radius for tight packing

        // Generate hexagonal grid lines
        for (q in -fieldSizeX until fieldSizeX) {
            for (r in -fieldSizeY until fieldSizeY) {
                // Convert axial coordinates to Cartesian for pointy-top hexagons
                val centerX = hexSize * 3.0f / 2.0f * q
                val centerY = hexSize * kotlin.math.sqrt(3.0f) * (r + q / 2.0f)

                // Generate vertices for a single hexagon
                for (i in 0 until numSides) {
                    val angle1 = 2.0 * Math.PI * i / numSides
                    var angle2 = 2.0 * Math.PI * (i + 1) / numSides
                    val x1 = centerX + radius * cos(angle1).toFloat()
                    val y1 = centerY + radius * sin(angle1).toFloat()
                    val x2 = centerX + radius * cos(angle2).toFloat()
                    val y2 = centerY + radius * sin(angle2).toFloat()
                    gridCoords.addAll(listOf(x1, y1, 0f, x2, y2, 0f))
                }
            }
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
        makeHexPrisms()
        lastFrameTime = System.nanoTime()
    }

    private fun makeHexPrisms() {
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

        synchronized(hexPrisms) {
            hexPrisms.clear()
            var prismCount = 0
            for (q in -fieldSizeX until fieldSizeX) {
                for (r in -fieldSizeY until fieldSizeY) {
                    // Axial to Cartesian coordinates, matching initializeGrid
                    val x = hexSize * 3.0f / 2.0f * q
                    val y = hexSize * kotlin.math.sqrt(3.0f) * (r + q / 2.0f)
                    var height = Random.nextFloat() * 7f
                    for (z in 0..height.toInt()) {
                        val color = colors.random()
                        val offset = 0.6f
                        hexPrisms.add(
                            HexPrism(
                                x,
                                y,
                                z.toFloat() * offset,
                                color
                            )
                        )
                        Log.d(TAG, "makeHexPrisms: Added prism $prismCount at ($x, $y, ${z * offset})")
                        prismCount++
                    }
                }
            }
            Log.d(TAG, "makeHexPrisms: Total prisms created: ${hexPrisms.size}")

            if (hexPrisms.isNotEmpty()) {
                val randomPrism = hexPrisms.random()
                val smallPrismSize = 0.2f
                smallHexPrism = HexPrism(
                    randomPrism.x,
                    randomPrism.y,
                    randomPrism.z + randomPrism.prismSize(),
                    floatArrayOf(1f, 1f, 1f, 1f),
                    smallPrismSize
                )
                parentHexPrism = randomPrism
                Log.d(TAG, "makeHexPrisms: Added small prism at (${smallHexPrism?.x}, ${smallHexPrism?.y}, ${smallHexPrism?.z}) on parent prism at (${parentHexPrism?.x}, ${parentHexPrism?.y}, ${parentHexPrism?.z})")
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

        if (hexPrisms.size == 0) {
            Log.d(TAG, "onDrawFrame: NO PRISMS !!!")
            makeHexPrisms()
        }

        drawGrid()

        synchronized(hexPrisms) {
            particles.removeAll { !it.update(deltaTime) }
            particles.forEach { it.draw(vpMatrix) }

            hexPrisms.forEach { it.updateFall(deltaTime) }
            smallHexPrism?.let { small ->
                parentHexPrism?.let { parent ->
                    if (parent.isFalling) {
                        small.z = parent.z + parent.prismSize()
                    }
                }
            }

            for (prism in hexPrisms) {
                prism.draw(vpMatrix)
            }
            smallHexPrism?.draw(vpMatrix)
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

        synchronized(hexPrisms) {
            if (hexPrisms.isEmpty()) {
                Log.d(TAG, "handleTouch: No prisms available")
                return closestIndex
            }

            hexPrisms.forEachIndexed { index, prism ->
                val distance = prism.intersectRayWithHexPrism(rayOrigin, rayDir)
                Log.d(TAG, "handleTouch: Prism $index at (${prism.x}, ${prism.y}, ${prism.z}), distance: ${distance ?: "null"}")
                if (distance != null && distance < minDistance) {
                    minDistance = distance
                    closestIndex = index
                    Log.d(TAG, "handleTouch: New closest prism index: $index at distance $distance")
                }
            }

            if (closestIndex >= 0) {
                val selectedPrismLocal = hexPrisms[closestIndex]
                selectedPrismLocal.randomizeColor()
                selectedPrism = selectedPrismLocal
                Log.d(TAG, "handleTouch: Selected prism index: $closestIndex at (${selectedPrismLocal.x}, ${selectedPrismLocal.y}, ${selectedPrismLocal.z})")

                smallHexPrism?.let {
                    it.x = selectedPrismLocal.x
                    it.y = selectedPrismLocal.y
                    it.z = selectedPrismLocal.z + selectedPrismLocal.prismSize()
                    parentHexPrism = selectedPrismLocal
                    Log.d(TAG, "handleTouch: Moved small prism to (${it.x}, ${it.y}, ${it.z}) on parent prism at (${parentHexPrism?.x}, ${parentHexPrism?.y}, ${parentHexPrism?.z})")
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
                            x = selectedPrismLocal.x,
                            y = selectedPrismLocal.y,
                            z = selectedPrismLocal.z,
                            color = selectedPrismLocal.getPrismColor(),
                            sizeScale = 0.2f,
                            velocity = velocity,
                            lifeTime = 3f
                        )
                    )
                }
                Log.d(TAG, "handleTouch: Created 20 particles at (${selectedPrismLocal.x}, ${selectedPrismLocal.y}, ${selectedPrismLocal.z}), particle count: ${particles.size}")

                hexPrisms.removeAt(closestIndex)
                Log.d(TAG, "handleTouch: Removed prism at index $closestIndex")

                if (selectedPrismLocal == parentHexPrism) {
                    parentHexPrism = null
                    Log.d(TAG, "handleTouch: Parent prism removed, parentHexPrism set to null")
                }

                val removedX = selectedPrismLocal.x
                val removedY = selectedPrismLocal.y
                val removedZ = selectedPrismLocal.z
                val offset = 0.6f
                val prismsAbove = hexPrisms.filter {
                    abs(it.x - removedX) < 0.01f && abs(it.y - removedY) < 0.01f && it.z > removedZ
                }
                prismsAbove.forEach { prism ->
                    prism.isFalling = true
                    prism.startZ = prism.z
                    prism.targetZ = prism.z - offset
                    prism.fallProgress = 0f
                    Log.d(TAG, "handleTouch: Prism at (${prism.x}, ${prism.y}, ${prism.z}) will fall to z=${prism.targetZ}")
                }

                selectedPrism = null
            } else {
                selectedPrism = null
                Log.d(TAG, "handleTouch: No prism selected")
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