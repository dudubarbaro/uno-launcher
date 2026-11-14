package io.github.dudubarbaro.unolauncher.trip

import io.github.dudubarbaro.unolauncher.obd.Sample

/** Razão ar/combustível e densidade (g/L) de cada combustível brasileiro. */
enum class Fuel(val key: String, val label: String, val afr: Double, val densityGPerL: Double) {
    GASOLINA("gasolina", "gasolina", 13.2, 750.0),
    ETANOL("etanol", "etanol", 9.0, 809.0);

    companion object {
        fun fromKey(k: String?): Fuel = entries.firstOrNull { it.key == k } ?: GASOLINA
    }
}

/** Ar admitido (g/s) pelo método velocidade-densidade, para carros sem sensor MAF. */
fun airGramsPerSecond(mapKpa: Double, iatC: Double, rpm: Double, displacementL: Double = 0.999, ve: Double = 0.80): Double =
    mapKpa * 1000 * (displacementL / 1000) * ve * (rpm / 120) / (287.05 * (iatC + 273.15)) * 1000

/** Consumo estimado em L/h; o [multiplier] é o ajuste fino feito pelo dono com um tanque cheio. */
fun fuelLitersPerHour(s: Sample, fuel: Fuel, multiplier: Double): Double {
    val rpm = s.rpm ?: return 0.0
    if (rpm <= 0) return 0.0
    val air = s.mafGs ?: run {
        val map = s.mapKpa ?: return 0.0
        val iat = s.iatC ?: return 0.0
        airGramsPerSecond(map.toDouble(), iat.toDouble(), rpm.toDouble())
    }
    return air / fuel.afr * 3600 / fuel.densityGPerL * multiplier
}
