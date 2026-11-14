package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import io.github.dudubarbaro.unolauncher.obd.DtcNames
import io.github.dudubarbaro.unolauncher.obd.Dtcs
import io.github.dudubarbaro.unolauncher.obd.Link

@Composable
fun DtcScreen(dtcs: Dtcs?, link: Link, busy: Boolean, onBack: () -> Unit, onRead: () -> Unit, onClear: () -> Unit) {
    val live = link == Link.CONNECTED || link == Link.DEMO
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(20.su), verticalArrangement = Arrangement.spacedBy(16.su)) {
        ScreenHeader("Códigos de falha", onBack)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.su)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    dtcs == null -> Center(if (live) "Toque em \"Ler de novo\"" else "Conecte o OBD2 para ler os códigos")
                    dtcs.count == 0 -> Center("Nenhum código de falha ✓")
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.su)) {
                        section("Armazenados", dtcs.stored)
                        section("Pendentes", dtcs.pending)
                        section("Permanentes", dtcs.permanent)
                    }
                }
            }
            Column(Modifier.width(280.su), verticalArrangement = Arrangement.spacedBy(14.su)) {
                UnoButton(if (busy) "Lendo…" else "Ler de novo", onRead, Modifier.fillMaxWidth(), filled = false, enabled = live && !busy)
                UnoButton("Apagar códigos", { confirm = true }, Modifier.fillMaxWidth(), enabled = live && !busy && (dtcs?.count ?: 0) > 0)
                Label("Apagar não conserta o defeito.", 14, color = TextDim)
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = Color(0xFF151517),
            title = { Label("Apagar códigos de falha?", 24, bold = true) },
            text = {
                Label(
                    "Apagar não conserta o defeito. Se o problema continuar, a luz de injeção acende de novo. " +
                        "Faça com a ignição ligada e o motor desligado.",
                    18,
                )
            },
            confirmButton = { UnoButton("Apagar", { confirm = false; onClear() }) },
            dismissButton = { UnoButton("Cancelar", { confirm = false }, filled = false) },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: String, codes: List<String>) {
    if (codes.isEmpty()) return
    item { Label(title, 16, color = TextDim) }
    items(codes) { code ->
        Row(
            Modifier.fillMaxWidth().card().padding(horizontal = 16.su, vertical = 12.su),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.su),
        ) {
            Label(code, 24, color = MaterialTheme.colorScheme.primary, bold = true)
            Label(DtcNames.describe(code), 17)
        }
    }
}

@Composable
private fun Center(text: String) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Label(text, 22, color = TextDim, align = TextAlign.Center) }
