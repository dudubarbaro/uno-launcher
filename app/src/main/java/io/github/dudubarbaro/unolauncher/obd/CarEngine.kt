package io.github.dudubarbaro.unolauncher.obd

/** Fonte dos dados do carro: o OBD2 real ou o modo demonstração. */
interface CarEngine {
    /** Roda até ser cancelado, reconectando sozinho. [onSample] é chamado uma vez por ciclo. */
    suspend fun run(onLink: (Link) -> Unit, onSample: (Sample) -> Unit)

    /** Nulo quando não está conectado. */
    suspend fun readDtcs(): Dtcs?

    suspend fun clearDtcs(): Boolean
}
