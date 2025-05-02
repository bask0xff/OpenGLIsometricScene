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
    private var parentCube: Cube? = null // Куб, на котором находится маленький кубик
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

        initializeGrid()

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

    private fun initializeGrid() {
        // Размер поля для сетки (можно настроить)
        val gridSizeX = fieldSizeX.toFloat()
        val gridSizeY = fieldSizeY.toFloat()
        val cellSize = 0.60f // Расстояние между кубами, совпадает с offset в вашем коде

        // Список координат для линий сетки
        val gridCoords = mutableListOf<Float>()

        // Горизонтальные линии (по Y)
        for (i in 0..fieldSizeX) {
            val x = i * cellSize
            gridCoords.addAll(listOf(
                x, 0f, 0f, // Начало линии
                x, gridSizeY * cellSize, 0f // Конец линии
            ))
        }

        // Вертикальные линии (по X)
        for (j in 0..fieldSizeY) {
            val y = j * cellSize
            gridCoords.addAll(listOf(
                0f, y, 0f, // Начало линии
                gridSizeX * cellSize, y, 0f // Конец линии
            ))
        }

        gridVertexCount = gridCoords.size / 3 // Количество вершин (x, y, z для каждой)

        // Инициализация буфера для сетки
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

        makeCubes()

        ball = Ball(1f)
        lastFrameTime = System.nanoTime()
    }

    private fun makeCubes() {
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

        // Создание программы для сетки
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
                gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0); // Белый цвет
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
                    randomCube.z + randomCube.cubeSize(), // На поверхности куба
                    floatArrayOf(1f, 1f, 1f, 1f),
                    smallCubeSize
                )
                parentCube = randomCube // Сохраняем куб, на котором находится маленький кубик
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

        if(cubes.size == 0){
            Log.d(TAG, "onDrawFrame: NO CUBES !!!")
            makeCubes()
        }

        // Отрисовка сетки
        drawGrid()

        synchronized(cubes) {
            cubes.forEach { it.updateFall(deltaTime) }

            // Обновляем позицию маленького кубика, если его родительский куб падает
            smallCube?.let { small ->
                parentCube?.let { parent ->
                    if (parent.isFalling) {
                        small.z = parent.z + parent.cubeSize()
                        Log.d(TAG, "onDrawFrame: Moved small cube to z=${small.z} due to parent cube falling to z=${parent.z}")
                    }
                }
            }

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
        //Log.d(TAG, "onDrawFrame: Rendered ${cubes.size} cubes and small cube at (${smallCube?.x}, ${smallCube?.y}, ${smallCube?.z})")
    }

    private fun drawGrid() {
        glUseProgram(gridProgram)

        // Передаем матрицу проекции и вида
        glUniformMatrix4fv(gridMVPMatrixHandle, 1, false, vpMatrix, 0)

        // Передаем координаты вершин
        gridBuffer?.let {
            it.position(0)
            glVertexAttribPointer(gridPositionHandle, 3, GL_FLOAT, false, 0, it)
            glEnableVertexAttribArray(gridPositionHandle)
        }

        // Отрисовка линий сетки
        glDrawArrays(GL_LINES, 0, gridVertexCount)

        // Отключаем атрибуты
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
                selectedCube?.setSelected(false)
                selectedCubeLocal.setSelected(true)
                selectedCubeLocal.randomizeColor()
                ballCube = selectedCubeLocal
                Log.d(TAG, "handleTouch: Selected cube index: $closestIndex at (${selectedCubeLocal.x}, ${selectedCubeLocal.y}, ${selectedCubeLocal.z})")

                // Перемещаем маленький кубик на выбранный куб
                smallCube?.let {
                    it.x = selectedCubeLocal.x
                    it.y = selectedCubeLocal.y
                    it.z = selectedCubeLocal.z + selectedCubeLocal.cubeSize()
                    parentCube = selectedCubeLocal // Обновляем родительский куб
                    Log.d(TAG, "handleTouch: Moved small cube to (${it.x}, ${it.y}, ${it.z}) on parent cube at (${parentCube?.x}, ${parentCube?.y}, ${parentCube?.z})")
                }

                // Удаляем куб
                cubes.removeAt(closestIndex)
                Log.d(TAG, "handleTouch: Removed cube at index $closestIndex")

                // Если удалён родительский куб, сбрасываем parentCube
                if (selectedCubeLocal == parentCube) {
                    parentCube = null
                    Log.d(TAG, "handleTouch: Parent cube removed, parentCube set to null")
                }

                // Находим кубы выше удалённого
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

                // Сбрасываем ballCube и selectedCube
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