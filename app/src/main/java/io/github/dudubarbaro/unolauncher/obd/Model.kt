package io.github.dudubarbaro.unolauncher.obd

/** Últimos valores lidos do carro. Nulo = ainda não lido ou o carro não informa. */
data class Sample(
    val rpm: Int? = null,
    val speedKmh: Int? = null,
    val coolantC: Int? = null,
    val batteryV: Double? = null,
    val mapKpa: Int? = null,
    val iatC: Int? = null,
    val mafGs: Double? = null,
)

data class Dtcs(val stored: List<String>, val pending: List<String>, val permanent: List<String>) {
    val count: Int get() = (stored + pending + permanent).distinct().size
}

enum class Link { OFF, CONNECTING, IGNITION_OFF, CONNECTED, DEMO }
