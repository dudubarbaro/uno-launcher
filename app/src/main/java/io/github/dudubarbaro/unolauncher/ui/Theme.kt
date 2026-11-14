package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.dudubarbaro.unolauncher.R

enum class Accent(val key: String, val label: String, val color: Color) {
    VERMELHO("vermelho", "Vermelho", Color(0xFFE10600)),
    BRANCO("branco", "Branco", Color(0xFFF2F2F2)),
    LARANJA("laranja", "Laranja", Color(0xFFFF6A00)),
    AZUL("azul", "Azul", Color(0xFF1E88FF));

    companion object {
        fun fromKey(k: String?): Accent = entries.firstOrNull { it.key == k } ?: VERMELHO
    }
}

val LocalAccent: ProvidableCompositionLocal<Color> = staticCompositionLocalOf { Accent.VERMELHO.color }
val LocalScale: ProvidableCompositionLocal<Float> = staticCompositionLocalOf { 1f }

val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_regular, FontWeight.Normal),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)

@Composable
fun UnoTheme(accent: Accent, content: @Composable () -> Unit) {
    val base = TextStyle(fontFamily = ChakraPetch, color = Color(0xFFEEEEEE))
    val t = Typography()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accent.color,
            onPrimary = if (accent == Accent.BRANCO) Color.Black else Color.White,
            background = Color.Black,
            surface = Color.Black,
            onBackground = Color(0xFFEEEEEE),
            onSurface = Color(0xFFEEEEEE),
        ),
        typography = Typography(
            displayLarge = t.displayLarge.merge(base), headlineMedium = t.headlineMedium.merge(base),
            titleLarge = t.titleLarge.merge(base), titleMedium = t.titleMedium.merge(base),
            bodyLarge = t.bodyLarge.merge(base), bodyMedium = t.bodyMedium.merge(base),
            labelLarge = t.labelLarge.merge(base), labelMedium = t.labelMedium.merge(base),
        ),
    ) {
        CompositionLocalProvider(LocalAccent provides accent.color, content = content)
    }
}

/** Preto com leve brilho, efeito black piano. */
fun Modifier.pianoBackground(): Modifier = this
    .background(Brush.radialGradient(0f to Color(0xFF1B1B1F), 0.55f to Color(0xFF09090B), 1f to Color.Black))
    .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.06f), 0.35f to Color.Transparent))
