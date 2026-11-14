package io.github.dudubarbaro.unolauncher.trip

import java.io.File

data class Trip(
    val startMs: Long,
    val endMs: Long,
    val km: Double,
    val seconds: Double,
    val liters: Double,
    val fuel: Fuel,
    val cost: Double,
)

/** Viagem atual e histórico em arquivos de texto, resistentes a corte de energia. */
class TripStore(private val dir: File) {
    private val current = File(dir, "current.txt")
    private val tmp = File(dir, "current.tmp")
    private val history = File(dir, "trips.csv")

    @Synchronized
    fun loadCurrent(): TripState? = runCatching {
        val p = current.readText().trim().split(';')
        TripState(p[0].toLong(), p[1].toDouble(), p[2].toDouble(), p[3].toDouble())
    }.getOrNull()

    @Synchronized
    fun saveCurrent(t: TripState) {
        dir.mkdirs()
        tmp.writeText("${t.startMs};${t.km};${t.seconds};${t.liters}\n")
        if (!tmp.renameTo(current)) { current.delete(); tmp.renameTo(current) }
    }

    @Synchronized
    fun finish(t: TripState, endMs: Long, fuel: Fuel, pricePerL: Double) {
        if (t.km >= 0.1) {
            dir.mkdirs()
            history.appendText("${t.startMs};$endMs;${t.km};${t.seconds};${t.liters};${fuel.key};${t.cost(pricePerL)}\n")
        }
        current.delete()
    }

    @Synchronized
    fun trips(): List<Trip> {
        if (!history.exists()) return emptyList()
        return history.readLines().mapNotNull { line ->
            runCatching {
                val p = line.split(';')
                Trip(p[0].toLong(), p[1].toLong(), p[2].toDouble(), p[3].toDouble(), p[4].toDouble(), Fuel.fromKey(p[5]), p[6].toDouble())
            }.getOrNull()
        }.sortedByDescending { it.startMs }
    }
}
