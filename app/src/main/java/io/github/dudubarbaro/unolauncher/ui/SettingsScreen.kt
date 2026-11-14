package io.github.dudubarbaro.unolauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.dudubarbaro.unolauncher.settings.Settings
import io.github.dudubarbaro.unolauncher.settings.parseDecimal
import io.github.dudubarbaro.unolauncher.sys.Perm
import io.github.dudubarbaro.unolauncher.trip.Fuel
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    s: Settings,
    granted: Set<Perm>,
    bonded: List<Pair<String, String>>,
    startAppLabel: String?,
    shortcutLabels: List<String>,
    customPhoto: Boolean,
    listState: LazyListState,
    onChange: ((Settings) -> Settings) -> Unit,
    onPickPhoto: () -> Unit,
    onDefaultPhoto: () -> Unit,
    onPickShortcut: (Int) -> Unit,
    onPerm: (Perm) -> Unit,
    onPickStartApp: () -> Unit,
    onApps: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.su), verticalArrangement = Arrangement.spacedBy(12.su)) {
        ScreenHeader("Configurações", onBack)
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(10.su)) {
            section("Tela inicial")
            item { NavRow("Foto do Uno", if (customPhoto) "Foto da galeria" else "Uno padrão · trocar pela galeria", onPickPhoto) }
            if (customPhoto) item { NavRow("Usar o Uno padrão", "Com as luzes acendendo quando o motor liga", onDefaultPhoto) }
            shortcutLabels.forEachIndexed { i, label -> item { NavRow("Atalho ${i + 1}", label) { onPickShortcut(i) } } }

            section("Carro")
            item {
                ChoiceRow("Combustível no tanque", Fuel.entries.map { it.label.replaceFirstChar(Char::uppercase) }, Fuel.entries.indexOf(s.fuel)) { i ->
                    onChange { it.copy(fuel = Fuel.entries[i]) }
                }
            }
            item { NumberRow("Preço do litro (R$)", s.pricePerL, 2) { v -> onChange { it.copy(pricePerL = v) } } }
            item { NumberRow("Ajuste fino do consumo (1,00 = sem ajuste)", s.fuelMultiplier, 2) { v -> onChange { it.copy(fuelMultiplier = v) } } }
            item { NumberRow("Hodômetro atual (km)", s.odometerKm, 0) { v -> onChange { it.copy(odometerKm = v) } } }
            item { NumberRow("Próxima revisão (km)", s.nextServiceKm, 0) { v -> onChange { it.copy(nextServiceKm = v) } } }
            item {
                if (bonded.isEmpty()) {
                    InfoRow("Adaptador OBD2", "Nenhum aparelho pareado, Bluetooth desligado ou sem permissão")
                } else {
                    val chosen = io.github.dudubarbaro.unolauncher.obd.pickAdapter(bonded, s.adapterAddress)
                    ChoiceRow("Adaptador OBD2", bonded.map { it.first }, bonded.indexOfFirst { it.second == chosen }) { i ->
                        onChange { it.copy(adapterAddress = bonded[i].second) }
                    }
                }
            }

            section("Ao ligar o carro")
            item { SwitchRow("Conectar no OBD2", s.startObd) { v -> onChange { it.copy(startObd = v) } } }
            item { SwitchRow("Retomar a música", s.startResumeMusic) { v -> onChange { it.copy(startResumeMusic = v) } } }
            item {
                SwitchRow("Abrir um app", s.startOpenApp != null, subtitle = startAppLabel ?: s.startOpenApp) { v ->
                    if (v) onPickStartApp() else onChange { it.copy(startOpenApp = null) }
                }
            }
            if (s.startOpenApp != null) item { NavRow("App que abre ao ligar", startAppLabel ?: s.startOpenApp, onPickStartApp) }

            section("Tela")
            item {
                ChoiceRow("Cor de destaque", Accent.entries.map { it.label }, Accent.entries.indexOf(s.accent), Accent.entries.map { it.color }) { i ->
                    onChange { it.copy(accent = Accent.entries[i]) }
                }
            }
            item { SwitchRow("Brilho automático (dia e noite)", s.autoBrightness, subtitle = "Pelo nascer e pôr do sol") { v -> onChange { it.copy(autoBrightness = v) } } }
            item { SliderRow("Brilho de dia", s.dayBrightness) { v -> onChange { it.copy(dayBrightness = v) } } }
            item { SliderRow("Brilho de noite", s.nightBrightness) { v -> onChange { it.copy(nightBrightness = v) } } }
            item { SwitchRow("Tela cheia", s.fullscreen, subtitle = "Esconde as barras do sistema") { v -> onChange { it.copy(fullscreen = v) } } }

            section("Alertas")
            item { NumberRow("Motor quente acima de (°C)", s.alertCoolantC.toDouble(), 0) { v -> onChange { it.copy(alertCoolantC = v.roundToInt()) } } }
            item { NumberRow("Bateria baixa abaixo de (V)", s.alertBatteryLowV, 1) { v -> onChange { it.copy(alertBatteryLowV = v) } } }
            item { NumberRow("Bateria alta acima de (V)", s.alertBatteryHighV, 1) { v -> onChange { it.copy(alertBatteryHighV = v) } } }
            item { NumberRow("Avisar revisão antes de (km)", s.alertServiceBeforeKm, 0) { v -> onChange { it.copy(alertServiceBeforeKm = v) } } }

            section("Teste")
            item {
                SwitchRow("Modo demonstração", s.demo, subtitle = "Dados simulados, para testar sem o carro") { v -> onChange { it.copy(demo = v) } }
            }

            section("Apps")
            item { NavRow("Todos os apps", "Abrir qualquer app instalado", onApps) }

            section("Permissões")
            Perm.entries.forEach { p ->
                item {
                    SettingRow(p.label) {
                        if (p in granted) Label("✓", 26, color = MaterialTheme.colorScheme.primary, bold = true)
                        else UnoButton("Liberar", { onPerm(p) })
                    }
                }
            }

            section("Sobre")
            item { NavRow("Sobre este aparelho", "Resolução, Android e eventos do sistema", onAbout) }
        }
    }
}

private fun LazyListScope.section(title: String) = item {
    Label(title, 22, Modifier.padding(top = 14.su, bottom = 2.su), color = MaterialTheme.colorScheme.primary, bold = true)
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.su).card()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 18.su, vertical = 10.su),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.su),
    ) {
        Column(Modifier.weight(1f)) {
            Label(title, 19)
            subtitle?.let { Label(it, 14, color = TextDim) }
        }
        trailing()
    }
}

@Composable
private fun InfoRow(title: String, text: String) = SettingRow(title, text) {}

@Composable
private fun NavRow(title: String, subtitle: String?, onClick: () -> Unit) =
    SettingRow(title, subtitle, onClick) { Label("›", 30, color = TextDim, bold = true) }

@Composable
private fun SwitchRow(title: String, checked: Boolean, subtitle: String? = null, onChange: (Boolean) -> Unit) =
    SettingRow(title, subtitle, { onChange(!checked) }) {
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary),
        )
    }

@Composable
private fun ChoiceRow(title: String, options: List<String>, selected: Int, swatches: List<Color>? = null, onPick: (Int) -> Unit) =
    SettingRow(title) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.su), verticalAlignment = Alignment.CenterVertically) {
            options.forEachIndexed { i, o ->
                if (swatches != null) {
                    Box(
                        Modifier.size(48.su).clip(CircleShape).background(swatches[i])
                            .border(if (i == selected) 4.dp else 1.dp, if (i == selected) Color.White else TileBorder, CircleShape)
                            .clickable { onPick(i) },
                    )
                } else {
                    UnoButton(o, { onPick(i) }, filled = i == selected)
                }
            }
        }
    }

@Composable
private fun SliderRow(title: String, value: Int, onChange: (Int) -> Unit) =
    SettingRow(title, "${(value * 100 / 255)}%") {
        Slider(
            value.toFloat(), { onChange(it.roundToInt()) }, Modifier.width(320.su), valueRange = 1f..255f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
        )
    }

/** Campo numérico: aceita vírgula; valor inválido volta ao salvo e avisa. */
@Composable
private fun NumberRow(title: String, value: Double, decimals: Int, onSet: (Double) -> Unit) {
    val shown = String.format(Locale("pt", "BR"), "%.${decimals}f", value)
    var text by remember(value) { mutableStateOf(shown) }
    var invalid by remember { mutableStateOf(false) }
    fun commit() {
        val v = parseDecimal(text)
        if (v == null) { invalid = true; text = shown } else { invalid = false; if (v != value) onSet(v) }
    }
    SettingRow(title, if (invalid) "valor inválido" else null) {
        OutlinedTextField(
            text, { text = it },
            Modifier.width(200.su).onFocusChanged { if (!it.isFocused && text != shown) commit() },
            singleLine = true,
            textStyle = TextStyle(fontFamily = ChakraPetch, fontSize = 20.st, color = TextMain),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = TileBorder),
        )
    }
}
