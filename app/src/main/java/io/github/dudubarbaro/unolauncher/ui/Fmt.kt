package io.github.dudubarbaro.unolauncher.ui

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Textos de tela em pt-BR, com vírgula decimal. */
object Fmt {
    private val br = Locale("pt", "BR")
    private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", br)

    fun temp(c: Int?): String = c?.let { "$it°C" } ?: "—"
    fun volts(v: Double?): String = v?.let { String.format(br, "%.1f V", it) } ?: "—"
    fun money(v: Double): String = String.format(br, "R$ %.2f", v)
    fun kmPerL(v: Double?): String = (v?.let { String.format(br, "%.1f", it) } ?: "—") + " km/l"
    fun km(v: Double): String = String.format(br, "%.1f km", v)
    fun liters(v: Double): String = String.format(br, "%.1f L", v)
    fun rpmK(rpm: Int?): String = rpm?.let { String.format(br, "%.1f", it / 1000.0) } ?: "—"

    fun duration(sec: Double): String {
        val m = (sec / 60).roundToInt()
        return if (m >= 60) String.format(Locale.ROOT, "%dh%02d", m / 60, m % 60) else "$m min"
    }

    fun clock(t: ZonedDateTime): String = String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
    fun date(t: ZonedDateTime): String = dateFmt.format(t).replace(".", "").lowercase(br)
}
