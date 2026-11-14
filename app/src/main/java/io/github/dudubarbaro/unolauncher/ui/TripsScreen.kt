package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.dudubarbaro.unolauncher.settings.Settings
import io.github.dudubarbaro.unolauncher.trip.Trip
import io.github.dudubarbaro.unolauncher.trip.TripState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val when_ = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR"))

@Composable
fun TripsScreen(current: TripState, trips: List<Trip>, settings: Settings, onBack: () -> Unit, onNewTrip: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.su), verticalArrangement = Arrangement.spacedBy(16.su)) {
        ScreenHeader("Viagens", onBack)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.su)) {
            Column(Modifier.weight(1f).fillMaxHeight().card().padding(20.su), verticalArrangement = Arrangement.spacedBy(10.su)) {
                Label("viagem atual · ${settings.fuel.label}", 16, color = TextDim)
                Label(Fmt.km(current.km), 40, bold = true)
                Label("${Fmt.duration(current.seconds)} · ${Fmt.liters(current.liters)} · ${Fmt.kmPerL(current.kmPerL())}", 18)
                Label(
                    if (settings.pricePerL > 0) Fmt.money(current.cost(settings.pricePerL)) else "defina o preço em Configurações",
                    26, color = MaterialTheme.colorScheme.primary, bold = true,
                )
                Box(Modifier.weight(1f))
                UnoButton("Nova viagem", onNewTrip, Modifier.fillMaxWidth())
            }
            Box(Modifier.weight(1.4f).fillMaxHeight()) {
                if (trips.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Label("Nenhuma viagem salva ainda", 20, color = TextDim) }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.su)) {
                        items(trips) { t ->
                            val kmL = if (t.liters >= 0.01) t.km / t.liters else null
                            Column(Modifier.fillMaxWidth().card().padding(horizontal = 16.su, vertical = 12.su)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Label(when_.format(Date(t.startMs)), 16, Modifier.weight(1f), color = TextDim)
                                    Label(Fmt.money(t.cost), 20, color = MaterialTheme.colorScheme.primary, bold = true)
                                }
                                Label("${Fmt.km(t.km)} · ${Fmt.duration(t.seconds)} · ${Fmt.liters(t.liters)} · ${Fmt.kmPerL(kmL)}", 18)
                            }
                        }
                    }
                }
            }
        }
        Label("litros e R$ são estimativas", 14, color = TextDim)
    }
}
