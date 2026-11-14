package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.RuntimeEnvironment

private val sizes = listOf(
    "w1024dp-h600dp-land-mdpi" to "1024x600",
    "w853dp-h480dp-land-hdpi" to "1280x720",
    "w1280dp-h800dp-land-mdpi" to "tablet",
)

/** Foto da tela nos 3 tamanhos (central 1024×600, central 1280×720 e tablet) em build/shots/. */
fun shoot(name: String, content: @Composable () -> Unit) {
    for ((q, tag) in sizes) {
        RuntimeEnvironment.setQualifiers(q)
        captureRoboImage("build/shots/$name-$tag.png") {
            UnoTheme(Accent.VERMELHO) {
                Box(Modifier.fillMaxSize().pianoBackground()) { ScaledRoot { content() } }
            }
        }
    }
}
