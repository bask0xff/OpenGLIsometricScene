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
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d(TAG, "onTouchEvent: $x , $y")
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            renderer.handleTouch(
                x, y,
                width, height
            )

            return true
        }
        return super.onTouchEvent(event)
    }
}
