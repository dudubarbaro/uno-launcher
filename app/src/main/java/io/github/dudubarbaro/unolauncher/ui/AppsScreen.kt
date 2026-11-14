package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

private val collator = java.text.Collator.getInstance(java.util.Locale("pt", "BR"))

data class AppEntry(val label: String, val pkg: String, val icon: ImageBitmap?)

/** Lista de todos os apps; também serve para escolher o app que abre ao ligar. */
@Composable
fun AppsScreen(title: String, apps: List<AppEntry>, onPick: (String) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = apps.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }.sortedWith(compareBy(collator) { it.label })
    Column(Modifier.fillMaxSize().padding(20.su), verticalArrangement = Arrangement.spacedBy(16.su)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { ScreenHeader(title, onBack) }
            OutlinedTextField(
                query, { query = it }, Modifier.width(320.su), singleLine = true,
                placeholder = { Label("Buscar", 18, color = TextDim) },
                textStyle = TextStyle(fontFamily = ChakraPetch, fontSize = 18.st, color = TextMain),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = TileBorder),
            )
        }
        LazyVerticalGrid(GridCells.Fixed(5), horizontalArrangement = Arrangement.spacedBy(12.su), verticalArrangement = Arrangement.spacedBy(12.su)) {
            items(shown, key = { it.pkg }) { app ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.su)).background(TileBg).clickable { onPick(app.pkg) }.padding(12.su),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.su),
                ) {
                    if (app.icon != null) {
                        Image(app.icon, null, Modifier.size(64.su))
                    } else {
                        Box(Modifier.size(64.su).clip(RoundedCornerShape(16.su)).background(TileBorder), contentAlignment = Alignment.Center) {
                            Label(app.label.take(1).uppercase(), 30, bold = true)
                        }
                    }
                    Label(app.label, 16, align = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}
