package io.github.dudubarbaro.unolauncher.trip

import io.github.dudubarbaro.unolauncher.obd.Sample

data class TripState(val startMs: Long = 0, val km: Double = 0.0, val seconds: Double = 0.0, val liters: Double = 0.0) {
    fun kmPerL(): Double? = if (liters < 0.01) null else km / liters
    fun cost(pricePerL: Double): Double = liters * pricePerL
}

/** Soma [dtSec] de viagem. Intervalos acima de 5 s (reconexão, central dormindo) não contam. */
fun advance(t: TripState, s: Sample, dtSec: Double, nowMs: Long, fuel: Fuel, multiplier: Double): TripState {
    val started = if (t.startMs == 0L) t.copy(startMs = nowMs) else t
    if (dtSec <= 0 || dtSec > 5.0) return started
    // ponytail: sem corte de combustível na desaceleração; o multiplicador absorve o erro
    return started.copy(
        km = started.km + (s.speedKmh ?: 0) * dtSec / 3600,
        seconds = started.seconds + dtSec,
        liters = started.liters + fuelLitersPerHour(s, fuel, multiplier) * dtSec / 3600,
    )
}
