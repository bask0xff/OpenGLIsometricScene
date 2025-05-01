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

class IsoGLRenderer : GLSurfaceView.Renderer {
    private val TAG = "IsoGLRenderer"
    private val cubes = mutableListOf<Cube>()
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
    // Обработчики шейдеров (вам нужно будет их добавить)
    private var positionHandle = 0
    private var uMVPMatrixHandle = 0

    private var rotationAngle: Float = 0f

    private var sphereCoords = FloatArray(1000) // Массив для шара

    // Матрица для проекции и видовых преобразований
    private val mMVPMatrix = FloatArray(16)
    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)

    private val triangleBuffer: FloatBuffer
    private val sphereBuffer: FloatBuffer

    private var ball = Ball(10f)
    private lateinit var testTriangle: TestTriangle
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val cube = Cube(0f,0f,0f, floatArrayOf(1f, 0f, 0f, 1f))
    private var ballCube: Cube? = null

    private val nearPointNdc = FloatArray(4)
    private val farPointNdc = FloatArray(4)

    var fieldSizeX = 5
    var fieldSizeY = 5
    var cubeSize = 0.5f


    private val heightMap = Array(10) { IntArray(10) }

    init {
        // Генерация случайных значений для каждой точки (высота башни)
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                heightMap[i][j] = (1..5).random()  // Высоты от 1 до 5
            }
        }
        // Генерация буфера для треугольника
        triangleBuffer = ByteBuffer.allocateDirect(triangleCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        triangleBuffer.put(triangleCoords).position(0)

        // Генерация данных для шара
        generateSphereCoordinates(1.0f)
        sphereBuffer = ByteBuffer.allocateDirect(sphereCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        sphereBuffer.put(sphereCoords).position(0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(0.2f, 0.2f, 0.2f, 1f)
        glEnable(GL_DEPTH_TEST)

        // Устанавливаем начальные значения для матриц
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

        val cells = 5
        for (x in 0..cells-1) {
            for (y in 0..cells-1) {
                var height = Random.nextFloat() * 1f  // Генерация случайной высоты от 0 до 5
                for (z in 0..height.toInt()) {
                    val color = colors.random()

                    height = z.toFloat()
                    // TODO: change it, based on cubeSize()
                    var offset = 0.63f
                    offset = 1f // for 0.5 size
                    offset = 0.20f
                    offset = 0.60f
                    //if(x == 0 || x == 4 || y == 0 || y == 4)
                    //if (Random.nextFloat() > 0.3f)
                        cubes.add(
                            Cube(
                                x.toFloat() * offset,
                                y.toFloat() * offset,
                                height,
                                color
                            )
                        )  // Добавляем куб с высотой
                }
            }
        }

        ball = Ball(1f)
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

        /*
        // Настройка проекционной матрицы
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, -5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0)
        */
    }

    // Рисование треугольника
    private fun drawTriangle() {
        // Код для рисования треугольника с использованием OpenGL
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, triangleBuffer)
        glEnableVertexAttribArray(positionHandle)
        glDrawArrays(GL_TRIANGLES, 0, 3)
    }

    // Рисование шара
    private fun drawSphere() {
        // Код для рисования шара с использованием OpenGL
        glVertexAttribPointer(positionHandle, 3, GL_FLOAT, false, 0, sphereBuffer)
        glEnableVertexAttribArray(positionHandle)
        glDrawArrays(GL_TRIANGLES, 0, sphereCoords.size / 3)
    }

    // Функция для генерации координат для сферы
    private fun generateSphereCoordinates(radius: Float) {
        val sphereList = mutableListOf<Float>() // Используем ArrayList для динамического добавления данных
        val slices = 20 // Количество срезов
        val stacks = 20 // Количество высот
        for (i in 0 until stacks) {
            val phi = Math.PI * (i / (stacks - 1).toDouble()) // Угол по высоте
            for (j in 0 until slices) {
                val theta = 2.0 * Math.PI * (j / slices.toDouble()) // Угол по окружности
                val x = (radius * Math.sin(phi) * Math.cos(theta)).toFloat()
                val y = (radius * Math.sin(phi) * Math.sin(theta)).toFloat()
                val z = (radius * Math.cos(phi)).toFloat()

                // Добавляем координаты в список
                sphereList.add(x)
                sphereList.add(y)
                sphereList.add(z)
            }
        }

        // Преобразуем список в массив
        sphereCoords = sphereList.toFloatArray()
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Отрисовываем башни
        for (i in 0 until 10) {
            for (j in 0 until 10) {
                val height = heightMap[i][j]
                for (k in 0 until height) {
                    val modelMatrix = FloatArray(16)
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, i.toFloat(), k.toFloat(), j.toFloat())

                    val mvpMatrix = FloatArray(16)
                    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

                    cube.draw(mvpMatrix) // Передаём финальную матрицу в отрисовку куба
                }
            }
        }


        // Логируем позицию мяча
        ballCube?.let {
            val basePosition = Vector3(it.x, it.y, it.z + 0.5f)
            //Log.d(TAG, "Ball position: ${basePosition.x}, ${basePosition.y}, ${basePosition.z}") // Позиция мяча в логе

            val modelMatrix = FloatArray(16)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, basePosition.x, basePosition.y, basePosition.z)
            Matrix.scaleM(modelMatrix, 0, 3.0f, 3.0f, 3.0f) // увеличиваем в 3 раза

            val mvpMatrix = FloatArray(16)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

            val positionArray = basePosition.toFloatArray()  // Преобразуем в FloatArray
            val color = floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f)  // Красный цвет для мяча
            //ball.draw(vpMatrix, color)  /
            //ball.draw(mvpMatrix, color)
            //testTriangle.draw(vpMatrix)


            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 2f, 2f, 0f) // Помещаем рядом с кубами
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

            //testTriangle.draw(mvpMatrix)

        }

        for (cube in cubes) {
            cube.draw(vpMatrix)
        }

        // Рисуем треугольник
        //drawTriangle()

        // Рисуем шар
        //()

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

        Log.d(TAG, "handleTouch: clsect cube: ${closestCube?.x}, ${closestCube?.y}")

        closestCube?.let {
            it.randomizeColor()
            ballCube = it
        }

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
}
