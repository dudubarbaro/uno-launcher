package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import io.github.dudubarbaro.unolauncher.car.Alert
import io.github.dudubarbaro.unolauncher.car.CarState
import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.settings.Settings

private const val RPM_MAX = 7000f

/** Painel esportivo do OBD2 (modelo A). */
@Composable
fun ObdScreen(
    car: CarState,
    settings: Settings,
    photo: ImageBitmap?,
    onBack: () -> Unit,
    onDtcs: () -> Unit,
    onTrips: () -> Unit,
    onConnect: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val s = car.sample
    Box(Modifier.fillMaxSize()) {
        if (photo != null) Image(photo, null, Modifier.fillMaxSize().alpha(0.25f), contentScale = ContentScale.Crop)
        Row(Modifier.fillMaxSize().padding(20.su), horizontalArrangement = Arrangement.spacedBy(20.su)) {
            BackButton(onBack)
            Box(Modifier.weight(1.3f).fillMaxHeight(), contentAlignment = Alignment.Center) { Gauge(s.rpm, s.speedKmh) }
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.su, Alignment.CenterVertically),
            ) {
                MetricBar("motor", Fmt.temp(s.coolantC), s.coolantC?.let { (it - 40) / 80f }, hot = Alert.MOTOR_QUENTE in car.alerts)
                MetricBar(
                    "bateria", Fmt.volts(s.batteryV), s.batteryV?.let { ((it - 10) / 6).toFloat() },
                    hot = Alert.BATERIA_BAIXA in car.alerts || Alert.BATERIA_ALTA in car.alerts,
                )
                Column(Modifier.fillMaxWidth().clickable(onClick = onTrips)) {
                    Label("viagem · ${settings.fuel.label}", 15, color = TextDim)
                    val km = Fmt.kmPerL(car.trip.kmPerL())
                    val money = if (settings.pricePerL > 0) Fmt.money(car.trip.cost(settings.pricePerL)) else "defina o preço"
                    Label("$money · $km", 24, bold = true, maxLines = 1)
                    Label("estimativa · toque para ver as viagens", 13, color = TextDim)
                }
                Row(
                    Modifier.fillMaxWidth().card().clickable(onClick = onDtcs).padding(horizontal = 16.su, vertical = 14.su),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Label("Códigos de falha", 18, Modifier.weight(1f))
                    Label(car.dtcs?.count?.toString() ?: "—", 24, color = accent, bold = true)
                }
                LinkStatus(car, onConnect)
            }
        }
    }
}

@Composable
private fun Gauge(rpm: Int?, speed: Int?) {
    val accent = MaterialTheme.colorScheme.primary
    val stroke = 16.su
    Box(Modifier.fillMaxHeight(0.9f).aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val arc = Size(size.width - w, size.height - w)
            val tl = Offset(w / 2, w / 2)
            drawArc(Color(0xFF222222), 135f, 270f, false, tl, arc, style = Stroke(w, cap = StrokeCap.Round))
            val f = ((rpm ?: 0) / RPM_MAX).coerceIn(0f, 1f)
            if (f > 0) drawArc(accent, 135f, 270f * f, false, tl, arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Label(Fmt.rpmK(rpm), 84, bold = true)
            Label("rpm x1000", 16, color = TextDim)
            Label(speed?.let { "$it km/h" } ?: "— km/h", 30, bold = true)
        }
    }
}

@Composable
private fun MetricBar(label: String, value: String, fraction: Float?, hot: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.su)) {
        Label(label, 15, color = TextDim)
        Label(value, 26, color = if (hot) MaterialTheme.colorScheme.primary else TextMain, bold = true)
        Bar(fraction ?: 0f)
    }
}

@Composable
private fun LinkStatus(car: CarState, onConnect: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    when (car.link) {
        Link.CONNECTED -> Label("● OBD2 conectado", 16, color = accent, bold = true)
        Link.CONNECTING -> Label("Conectando…", 16, color = TextDim)
        Link.IGNITION_OFF -> Label("Ligue a ignição", 18, color = accent, bold = true)
        Link.DEMO -> Label("Modo demonstração", 16, color = accent, bold = true)
        Link.OFF -> Column(verticalArrangement = Arrangement.spacedBy(8.su)) {
            Label(car.problem ?: "OBD2 desconectado", 16, color = TextDim)
            UnoButton("Conectar", onConnect, Modifier.fillMaxWidth())
        }
    }
}
