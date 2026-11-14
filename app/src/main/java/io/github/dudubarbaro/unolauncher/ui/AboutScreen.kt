package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Dados do aparelho e eventos do sistema, para descobrir resolução e sinal de ignição da central. */
@Composable
fun AboutScreen(info: List<Pair<String, String>>, events: List<String>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.su), verticalArrangement = Arrangement.spacedBy(16.su)) {
        ScreenHeader("Sobre este aparelho", onBack)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.su)) {
            Column(
                Modifier.weight(1f).fillMaxHeight().card().verticalScroll(rememberScrollState()).padding(20.su),
                verticalArrangement = Arrangement.spacedBy(12.su),
            ) {
                info.forEach { (k, v) ->
                    Column {
                        Label(k, 15, color = TextDim)
                        Label(v, 22, bold = true)
                    }
                }
            }
            Column(Modifier.weight(1.4f).fillMaxHeight().card().padding(20.su), verticalArrangement = Arrangement.spacedBy(8.su)) {
                Label("Eventos do sistema (mais novo primeiro)", 16, color = TextDim)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.su)) {
                    items(events) { Label(it, 16) }
                }
            }
        }
    }
}
