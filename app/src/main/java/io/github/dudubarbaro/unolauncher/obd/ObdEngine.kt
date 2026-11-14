package io.github.dudubarbaro.unolauncher.obd

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** OBD2 real por um ELM327. Não troca de dispatcher: quem chama roda em Dispatchers.IO. */
class ObdEngine(private val open: () -> ElmSession) : CarEngine {
    private val mutex = Mutex()
    @Volatile private var session: ElmSession? = null
    private var supported: Set<Int> = emptySet()
    private var can = false

    /** Rápidos (RPM e velocidade) em todo ciclo; um lento por ciclo, em rodízio, para não engasgar o clone. */
    internal fun pollPlan(tick: Int, supported: Set<Int>): List<String> {
        val slow = buildList {
            add("0105"); add("ATRV")
            if (0x10 in supported) add("0110") else {
                if (0x0B in supported) add("010B")
                if (0x0F in supported) add("010F")
            }
        }
        return listOf("010C", "010D", slow[tick % slow.size])
    }

    override suspend fun run(onLink: (Link) -> Unit, onSample: (Sample) -> Unit) {
        var backoff = MIN_BACKOFF
        var s: ElmSession? = null
        try {
            while (true) {
                onLink(Link.CONNECTING)
                try {
                    s = open()
                    while (true) {
                        while (!initialize(s)) { onLink(Link.IGNITION_OFF); delay(5000) }
                        session = s
                        backoff = MIN_BACKOFF
                        onLink(Link.CONNECTED)
                        var last = Sample()
                        var tick = 0
                        var silent = 0
                        // A central parou de responder (ignição desligada com o adaptador ainda ligado): volta a inicializar.
                        while (silent < SILENT_CYCLES) {
                            mutex.withLock { for (c in pollPlan(tick++, supported)) last = merge(last, c, s.cmd(c)) }
                            silent = if (last.rpm == null && last.speedKmh == null) silent + 1 else 0
                            onSample(last)
                            delay(100)
                        }
                        session = null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // IOException do Bluetooth, mas também SecurityException/IllegalArgumentException do Android:
                    // nada aqui pode derrubar a tela inicial.
                    session = null
                    runCatching { s?.close() }
                    s = null
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF)
                }
            }
        } finally {
            session = null
            runCatching { s?.close() }
        }
    }

    private suspend fun initialize(s: ElmSession): Boolean {
        s.cmd("ATZ", 3000)
        for (c in listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")) s.cmd(c)
        val r = s.cmd("0100", 10_000) // ATSP0 procura o protocolo, pode demorar
        if (Obd.unableToConnect(r) || Obd.pidData(r, 0x00) == null) return false
        supported = Obd.supportedPids(r)
        can = Obd.isCan(s.cmd("ATDPN"))
        return true
    }

    private fun merge(last: Sample, cmd: String, raw: String): Sample = when (cmd) {
        "010C" -> last.copy(rpm = Obd.rpm(raw))
        "010D" -> last.copy(speedKmh = Obd.speedKmh(raw))
        "0105" -> last.copy(coolantC = Obd.coolantC(raw))
        "ATRV" -> last.copy(batteryV = Obd.voltage(raw))
        "010B" -> last.copy(mapKpa = Obd.mapKpa(raw))
        "010F" -> last.copy(iatC = Obd.iatC(raw))
        "0110" -> last.copy(mafGs = Obd.mafGs(raw))
        else -> last
    }

    override suspend fun readDtcs(): Dtcs? = mutex.withLock {
        val s = session ?: return null
        try {
            Dtcs(
                Obd.dtcs(s.cmd("03", 5000), 0x43, can),
                Obd.dtcs(s.cmd("07", 5000), 0x47, can),
                Obd.dtcs(s.cmd("0A", 5000), 0x4A, can),
            )
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun clearDtcs(): Boolean = mutex.withLock {
        val s = session ?: return false
        try {
            "44" in s.cmd("04", 5000).uppercase().replace(" ", "")
        } catch (e: IOException) {
            false
        }
    }

    private companion object {
        const val MIN_BACKOFF = 2_000L
        const val MAX_BACKOFF = 30_000L
        const val SILENT_CYCLES = 3
    }
}
