package io.github.dudubarbaro.unolauncher.car

import io.github.dudubarbaro.unolauncher.Graph
import io.github.dudubarbaro.unolauncher.obd.CarEngine
import io.github.dudubarbaro.unolauncher.obd.Dtcs
import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.obd.Sample
import io.github.dudubarbaro.unolauncher.trip.TripState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CarState(
    val link: Link = Link.OFF,
    val sample: Sample = Sample(),
    val dtcs: Dtcs? = null,
    val trip: TripState = TripState(),
    val alerts: Set<Alert> = emptySet(),
    /** Por que não dá para conectar (permissão, Bluetooth, adaptador). */
    val problem: String? = null,
)

/** Motor ligado: OBD2 respondendo (ou demo) e rotação acima de zero. Meia chave = rotação 0. */
val CarState.engineRunning: Boolean
    get() = (link == Link.CONNECTED || link == Link.DEMO) && (sample.rpm ?: 0) > 0

/** Ponto de encontro entre o serviço do OBD2 e as telas. */
object CarRepo {
    private val _state = MutableStateFlow(CarState())
    val state: StateFlow<CarState> = _state.asStateFlow()

    @Volatile var engine: CarEngine? = null
    @Volatile var onNewTrip: (() -> Unit)? = null

    fun set(f: (CarState) -> CarState) = _state.update(f)

    suspend fun readDtcs(): Dtcs? {
        val d = engine?.readDtcs() ?: return null
        set { it.copy(dtcs = d) }
        return d
    }

    suspend fun clearDtcs(): Boolean {
        val ok = engine?.clearDtcs() == true
        if (ok) readDtcs()
        return ok
    }

    /** Fecha a viagem atual no histórico e começa outra. */
    fun newTrip() {
        onNewTrip?.let { it(); return }
        val s = Graph.settings.value
        Graph.trips.loadCurrent()?.let { Graph.trips.finish(it, System.currentTimeMillis(), s.fuel, s.pricePerL) }
        set { it.copy(trip = TripState()) }
    }
}
