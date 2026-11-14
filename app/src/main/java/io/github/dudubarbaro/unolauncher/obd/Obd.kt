package io.github.dudubarbaro.unolauncher.obd

/** Leitura das respostas de texto de um ELM327 (com ou sem espaços, com ou sem eco). */
object Obd {
    private val hexOnly = Regex("^[0-9A-F]+$")
    private val canLengthLine = Regex("^[0-9A-F]{3}$")
    private val frameIndex = Regex("^[0-9A-F]:")
    private val volts = Regex("(\\d+(?:\\.\\d+)?)V")

    private fun lines(raw: String): List<String> = raw.uppercase()
        .replace(" ", "").replace(">", "").replace("SEARCHING...", "")
        .split('\r', '\n').map { it.trim() }.filter { it.isNotEmpty() }

    private fun bytes(hex: String): IntArray? =
        if (hex.length % 2 != 0 || !hexOnly.matches(hex)) null
        else IntArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16) }

    /** Bytes de dados da primeira resposta do modo 01 para [pid]; nulo se não houver. */
    fun pidData(raw: String, pid: Int): IntArray? {
        val prefix = "41%02X".format(pid)
        val line = lines(raw).firstOrNull { it.startsWith(prefix) } ?: return null
        return bytes(line.substring(prefix.length))?.takeIf { it.isNotEmpty() }
    }

    fun rpm(raw: String): Int? = pidData(raw, 0x0C)?.takeIf { it.size >= 2 }?.let { (it[0] * 256 + it[1]) / 4 }
    fun speedKmh(raw: String): Int? = pidData(raw, 0x0D)?.let { it[0] }
    fun coolantC(raw: String): Int? = pidData(raw, 0x05)?.let { it[0] - 40 }
    fun mapKpa(raw: String): Int? = pidData(raw, 0x0B)?.let { it[0] }
    fun iatC(raw: String): Int? = pidData(raw, 0x0F)?.let { it[0] - 40 }
    fun mafGs(raw: String): Double? = pidData(raw, 0x10)?.takeIf { it.size >= 2 }?.let { (it[0] * 256 + it[1]) / 100.0 }
    fun voltage(raw: String): Double? = volts.find(raw.uppercase().replace(" ", ""))?.groupValues?.get(1)?.toDouble()

    /** PIDs 0x01..0x20 que o carro informa, pela resposta do `0100`. */
    fun supportedPids(raw: String): Set<Int> {
        val b = pidData(raw, 0x00)?.takeIf { it.size >= 4 } ?: return emptySet()
        return (0 until 32).filter { i -> b[i / 8] and (0x80 shr (i % 8)) != 0 }.map { it + 1 }.toSet()
    }

    /** Resposta do `ATDPN`: protocolos 6 a 9 são CAN. */
    fun isCan(dpnRaw: String): Boolean =
        lines(dpnRaw).firstOrNull()?.removePrefix("A")?.firstOrNull()?.let { it in '6'..'9' } ?: false

    /** O adaptador responde, mas a central do carro não (ignição desligada). */
    fun unableToConnect(raw: String): Boolean {
        val r = raw.uppercase().replace(" ", "")
        return "UNABLETOCONNECT" in r || ("BUSINIT" in r && "ERROR" in r)
    }

    /** Códigos de falha dos modos 03/07/0A; [responseMode] é 0x43, 0x47 ou 0x4A. */
    fun dtcs(raw: String, responseMode: Int, isCan: Boolean): List<String> {
        val mode = "%02X".format(responseMode)
        val ls = lines(raw).filterNot { canLengthLine.matches(it) }.map { it.replace(frameIndex, "") }
        val out = mutableListOf<String>()
        fun pair(hex: String, at: Int) {
            val hi = hex.substring(at, at + 2).toInt(16)
            val lo = hex.substring(at + 2, at + 4).toInt(16)
            if (hi != 0 || lo != 0) out += decodeDtc(hi, lo)
        }
        if (!isCan) {
            for (l in ls) {
                if (!l.startsWith(mode) || !hexOnly.matches(l)) continue
                var at = 2
                while (at + 4 <= l.length) { pair(l, at); at += 4 }
            }
        } else {
            val all = ls.filter { hexOnly.matches(it) }.joinToString("")
            var at = 0
            while (at + 4 <= all.length && all.startsWith(mode, at)) {
                val n = all.substring(at + 2, at + 4).toInt(16)
                at += 4
                repeat(n) { if (at + 4 <= all.length) { pair(all, at); at += 4 } }
            }
        }
        return out
    }

    fun decodeDtc(hi: Int, lo: Int): String =
        "PCBU"[hi shr 6] + ((hi shr 4) and 3).toString() + "%X%02X".format(hi and 0xF, lo)
}
