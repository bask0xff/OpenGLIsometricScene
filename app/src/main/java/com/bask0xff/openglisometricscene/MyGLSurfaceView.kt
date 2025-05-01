package com.bask0xff.openglisometricscene

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val TAG = "MyGLSurfaceView"
    private val renderer: IsoGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = IsoGLRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        Log.d(TAG, "MyGLSurfaceView: Initialized")
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            Log.d(TAG, "onTouchEvent: Touch at ($x, $y), screen size: $width x $height")

            val cubeIndex = renderer.handleTouch(x, y, width, height)
            Log.d(TAG, "onTouchEvent: handleTouch returned index: $cubeIndex")
            if (cubeIndex >= 0) {
                Log.d(TAG, "onTouchEvent: Clicked cube with index: $cubeIndex")
            } else {
                Log.d(TAG, "onTouchEvent: No cube clicked")
            }

            return true
        }
        Log.d(TAG, "onTouchEvent: Ignored event: ${event.action}")
        return super.onTouchEvent(event)
    }
}