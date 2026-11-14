package io.github.dudubarbaro.unolauncher.obd

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Dados simulados de um trajeto, para testar no tablet sem o carro. */
class DemoEngine(private val tickMs: Long = 250) : CarEngine {
    private val stored = mutableListOf("P0171")
    private val pending = mutableListOf("P0300")

    override suspend fun run(onLink: (Link) -> Unit, onSample: (Sample) -> Unit) {
        onLink(Link.DEMO)
        var tick = 0L
        while (true) {
            val ms = tick * tickMs
            val t = tick++ * tickMs / 1000.0
            if (ms < HALF_KEY_MS) {
                onSample(Sample(rpm = 0, speedKmh = 0, coolantC = 60, batteryV = 12.5, mapKpa = 95, iatC = 30))
                delay(tickMs)
                continue
            }
            val speed = (55 + 45 * sin(t / 25)).coerceIn(0.0, 120.0)
            onSample(
                Sample(
                    rpm = (850 + speed * 38).toInt(),
                    speedKmh = speed.toInt(),
                    coolantC = (60 + 30 * min(t / 120, 1.0)).toInt(),
                    batteryV = 14.1 + 0.1 * sin(t / 7),
                    mapKpa = (35 + 25 * abs(sin(t / 9))).toInt(),
                    iatC = 30,
                ),
            )
            delay(tickMs)
        }
    }

    companion object {
        /** Primeiros segundos em meia chave (motor parado), para ver as luzes acenderem na partida. */
        const val HALF_KEY_MS = 6_000L
    }

    override suspend fun readDtcs(): Dtcs = synchronized(this) { Dtcs(stored.toList(), pending.toList(), emptyList()) }

    override suspend fun clearDtcs(): Boolean = synchronized(this) { stored.clear(); pending.clear(); true }
}
