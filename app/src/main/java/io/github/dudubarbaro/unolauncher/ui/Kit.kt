package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.dudubarbaro.unolauncher.car.Alert
import kotlin.math.min

val TextMain = Color(0xFFEEEEEE)
val TextDim = Color(0xFF8A8A8A)
val TileBg = Color(0xFF141416)
val TileBorder = Color(0xFF2A2A2A)

/** Medida escalada pelo tamanho da tela (1,0 = central de 1024×600). */
val Number.su: Dp @Composable get() = (toFloat() * LocalScale.current).dp

/** Tamanho de texto escalado; ignora a escala de fonte do sistema para o layout não quebrar. */
val Number.st: TextUnit @Composable get() = with(LocalDensity.current) { (toFloat() * LocalScale.current).dp.toSp() }

/** Fornece [LocalScale] = min(largura/1024, altura/600), entre 0,7 e 1,6. */
@Composable
fun ScaledRoot(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = min(maxWidth.value / 1024f, maxHeight.value / 600f).coerceIn(0.7f, 1.6f)
        CompositionLocalProvider(LocalScale provides scale) { content() }
    }
}

@Composable
fun Label(
    text: String,
    size: Number,
    modifier: Modifier = Modifier,
    color: Color = TextMain,
    bold: Boolean = false,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) = Text(
    text, modifier, color = color, fontSize = size.st, fontFamily = ChakraPetch,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = align,
    maxLines = maxLines, overflow = TextOverflow.Ellipsis, lineHeight = (size.toFloat() * 1.15f).st,
)

@Composable
fun Modifier.card(): Modifier = this
    .clip(RoundedCornerShape(10.su))
    .background(Color.White.copy(alpha = 0.025f))
    .border(1.dp, LocalAccent.current.copy(alpha = 0.33f), RoundedCornerShape(10.su))

/** Atalho grande da tela inicial. */
@Composable
fun Tile(label: String, glyph: String, highlight: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.su))
            .background(TileBg)
            .border(if (highlight) 2.dp else 1.dp, if (highlight) accent else TileBorder, RoundedCornerShape(12.su))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(glyph, 34, color = if (highlight) accent else TextMain, bold = true)
        Label(label, 18, maxLines = 1)
    }
}

@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(5.su).clip(RoundedCornerShape(3.su)).background(Color(0xFF333333))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(5.su).background(LocalAccent.current))
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier.size(64.su).clip(RoundedCornerShape(12.su)).background(LocalAccent.current).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val fg = MaterialTheme.colorScheme.onPrimary
        Canvas(Modifier.size(26.su)) {
            val w = size.width
            drawPath(
                Path().apply { moveTo(w * 0.68f, w * 0.12f); lineTo(w * 0.28f, w * 0.5f); lineTo(w * 0.68f, w * 0.88f) },
                fg, style = Stroke(w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Selo fixo do modo demonstração; quem chama posiciona. */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(6.su)).background(LocalAccent.current).padding(horizontal = 12.su, vertical = 4.su)) {
        Label("DEMO", 16, color = MaterialTheme.colorScheme.onPrimary, bold = true)
    }
}

@Composable
fun AlertBanner(alerts: Set<Alert>, modifier: Modifier = Modifier) {
    if (alerts.isEmpty()) return
    Box(modifier.fillMaxWidth().background(LocalAccent.current).padding(vertical = 10.su), contentAlignment = Alignment.Center) {
        Label("⚠ " + alerts.joinToString(" · ") { it.text }, 22, color = MaterialTheme.colorScheme.onPrimary, bold = true)
    }
}

/** Botão comum (texto), com alvo de toque grande. */
@Composable
fun UnoButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true, enabled: Boolean = true) {
    val accent = LocalAccent.current
    val bg = if (!enabled) Color(0xFF222222) else if (filled) accent else TileBg
    val fg = if (!enabled) TextDim else if (filled) MaterialTheme.colorScheme.onPrimary else TextMain
    Box(
        modifier
            .height(56.su)
            .clip(RoundedCornerShape(10.su))
            .background(bg)
            .border(1.dp, if (filled || !enabled) Color.Transparent else accent, RoundedCornerShape(10.su))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.su),
        contentAlignment = Alignment.Center,
    ) { Label(text, 18, color = fg, bold = true, maxLines = 1) }
}

/** Cabeçalho das telas internas: voltar + título. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        BackButton(onBack)
        Box(Modifier.size(16.su))
        Label(title, 30, bold = true)
    }
}
