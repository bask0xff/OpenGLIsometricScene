package com.bask0xff.openglisometricscene

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bask0xff.openglisometricscene.ui.theme.OpenGlIsometricSceneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenGLScene()
            /*
            Surface(modifier = Modifier.fillMaxSize()) {
                AndroidView(factory = { context ->
                    IsoGLSurfaceView(context)
                })
            }*/
        }
    }

    @Composable
    fun OpenGLScene() {
        AndroidView(
            factory = { context ->
                MyGLSurfaceView(context)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
