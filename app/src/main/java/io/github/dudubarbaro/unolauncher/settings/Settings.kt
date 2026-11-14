package io.github.dudubarbaro.unolauncher.settings

import io.github.dudubarbaro.unolauncher.trip.Fuel
import io.github.dudubarbaro.unolauncher.ui.Accent

data class Settings(
    // Carro
    val fuel: Fuel = Fuel.GASOLINA,
    val pricePerL: Double = 0.0,
    val fuelMultiplier: Double = 1.0,
    val odometerKm: Double = 0.0,
    val nextServiceKm: Double = 0.0,
    val adapterAddress: String? = null,
    // Ao ligar
    val startOpenApp: String? = null,
    val startResumeMusic: Boolean = true,
    val startObd: Boolean = true,
    // Tela
    val accent: Accent = Accent.VERMELHO,
    val autoBrightness: Boolean = false,
    val dayBrightness: Int = 255,
    val nightBrightness: Int = 60,
    val fullscreen: Boolean = false,
    // Alertas
    val alertCoolantC: Int = 105,
    val alertBatteryLowV: Double = 12.0,
    val alertBatteryHighV: Double = 15.0,
    val alertServiceBeforeKm: Double = 500.0,
    // Teste e localização aproximada (2 casas)
    val demo: Boolean = false,
    val lat: Double? = null,
    val lon: Double? = null,
)

/** Aceita vírgula ou ponto; recusa vazio, negativo e lixo. */
fun parseDecimal(s: String): Double? {
    val t = s.trim().replace(',', '.')
    if (t.count { it == '.' } > 1) return null
    return t.toDoubleOrNull()?.takeIf { it >= 0 && it.isFinite() }
}
