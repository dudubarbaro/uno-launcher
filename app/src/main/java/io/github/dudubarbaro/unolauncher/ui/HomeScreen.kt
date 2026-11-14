package io.github.dudubarbaro.unolauncher.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import io.github.dudubarbaro.unolauncher.media.NowPlaying

data class HomeUi(
    val clock: String,
    val date: String,
    val weather: String?,
    val coolant: String,
    val battery: String,
    val obdHint: String?,
    val music: NowPlaying?,
    val musicAccess: Boolean,
    val photo: ImageBitmap?,
)

private val photoEmpty = Brush.linearGradient(listOf(Color(0xFF2A2A2E), Color(0xFF0C0C0E)))

/** Tela inicial (modelo B): foto do Uno em destaque, música à direita e 4 atalhos embaixo. */
@Composable
fun HomeScreen(
    ui: HomeUi,
    onWaze: () -> Unit,
    onSpotify: () -> Unit,
    onObd: () -> Unit,
    onSettings: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onGrantMusic: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tileH = maxHeight * 0.22f
        Column(Modifier.fillMaxSize().padding(horizontal = 24.su, vertical = 20.su), verticalArrangement = Arrangement.spacedBy(16.su)) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.su)) {
                PhotoCard(ui, Modifier.weight(1.6f).fillMaxHeight())
                MusicCard(ui, onPlayPause, onNext, onPrev, onGrantMusic, Modifier.weight(1f).fillMaxHeight())
            }
            Row(Modifier.fillMaxWidth().height(tileH), horizontalArrangement = Arrangement.spacedBy(16.su)) {
                Tile("Waze", "➤", false, onWaze, Modifier.weight(1f).fillMaxHeight())
                Tile("Spotify", "♫", false, onSpotify, Modifier.weight(1f).fillMaxHeight())
                Tile("OBD2", "◉", true, onObd, Modifier.weight(1f).fillMaxHeight())
                Tile("Configurações", "⚙", false, onSettings, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun PhotoCard(ui: HomeUi, modifier: Modifier) {
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Box(modifier.clip(RoundedCornerShape(10.su)).background(photoEmpty)) {
        if (ui.photo != null) {
            Image(ui.photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Label(
                "Escolha a foto do seu Uno\nem Configurações", 18, Modifier.align(Alignment.Center).padding(24.su),
                color = TextDim, align = TextAlign.Center,
            )
        }
        // Sombra para ler hora e números por cima de qualquer foto.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.45f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f))))
        Column(Modifier.align(Alignment.TopStart).padding(20.su)) {
            Label(ui.clock, 64, bold = true)
            Label(listOfNotNull(ui.date, ui.weather).joinToString(" · "), 20, color = TextMain)
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(20.su),
            horizontalArrangement = Arrangement.spacedBy(20.su),
            verticalAlignment = Alignment.Bottom,
        ) {
            ui.obdHint?.let { Label(it, 15, color = TextDim) }
            Metric(ui.coolant, "motor", accent)
            Metric(ui.battery, "bateria", accent)
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.4f).height(4.su).background(accent))
    }
}

@Composable
private fun Metric(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Label(value, 28, color = color, bold = true)
        Label(label, 14, color = TextMain)
    }
}

@Composable
private fun MusicCard(
    ui: HomeUi,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onGrant: () -> Unit,
    modifier: Modifier,
) {
    val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary
    Column(modifier.card().padding(16.su), verticalArrangement = Arrangement.spacedBy(10.su)) {
        val m = ui.music
        when {
            !ui.musicAccess -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Label("♫", 64, color = TextDim) }
                Label("Para mostrar a música, libere o acesso", 16, color = TextDim)
                UnoButton("Liberar música", onGrant, Modifier.fillMaxWidth())
            }
            m == null -> {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Label("♫", 64, color = TextDim) }
                Label("Nada tocando", 20, bold = true)
                Label("Abra o Spotify ou outro app de música", 15, color = TextDim)
            }
            else -> {
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.su)).background(photoEmpty),
                    contentAlignment = Alignment.Center,
                ) {
                    if (m.art != null) Image(m.art.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Label("♫", 64, color = TextDim)
                }
                Label(m.title.ifBlank { "Sem título" }, 20, bold = true, maxLines = 1)
                Label(m.artist, 15, color = TextDim, maxLines = 1)
                val pos = m.positionAt(SystemClock.elapsedRealtime())
                Bar(if (m.durationMs > 0) pos.toFloat() / m.durationMs else 0f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    Control("⏮", TextMain, onPrev)
                    Control(if (m.playing) "❚❚" else "▶", accent, onPlayPause)
                    Control("⏭", TextMain, onNext)
                }
            }
        }
    }
}

@Composable
private fun Control(glyph: String, color: Color, onClick: () -> Unit) {
    Box(Modifier.size(64.su).clip(RoundedCornerShape(32.su)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Label(glyph, 32, color = color, bold = true)
    }
}
