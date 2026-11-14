package io.github.dudubarbaro.unolauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import io.github.dudubarbaro.unolauncher.ui.Accent
import io.github.dudubarbaro.unolauncher.ui.UnoTheme
import io.github.dudubarbaro.unolauncher.ui.pianoBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnoTheme(Accent.VERMELHO) {
                Box(Modifier.fillMaxSize().pianoBackground()) { Text("Uno Launcher") }
            }
        }
    }
}
