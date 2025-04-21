package com.bask0xff.openglisometricscene

import android.content.Context
import android.opengl.GLSurfaceView

class IsoGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: IsoGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = IsoGLRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

