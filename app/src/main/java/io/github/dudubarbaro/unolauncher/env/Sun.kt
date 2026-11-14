package io.github.dudubarbaro.unolauncher.env

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Nascer e pôr do sol calculados no aparelho (equação da NOAA), sem internet. */
object Sun {
    private val dawn = LocalTime.of(6, 0)
    private val dusk = LocalTime.of(18, 0)

    fun times(date: LocalDate, lat: Double, lon: Double, zone: ZoneId): Pair<ZonedDateTime, ZonedDateTime>? {
        val g = 2 * PI / 365 * (date.dayOfYear - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g))
        val decl = 0.006918 - 0.399912 * cos(g) + 0.070257 * sin(g) - 0.006758 * cos(2 * g) +
            0.000907 * sin(2 * g) - 0.002697 * cos(3 * g) + 0.00148 * sin(3 * g)
        val latR = Math.toRadians(lat)
        val x = cos(Math.toRadians(90.833)) / (cos(latR) * cos(decl)) - tan(latR) * tan(decl)
        if (x < -1 || x > 1) return null // sol da meia-noite ou noite polar
        val ha = Math.toDegrees(acos(x))
        val midnightUtc = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        fun at(minutes: Double): ZonedDateTime = Instant.ofEpochMilli(midnightUtc.toEpochMilli() + (minutes * 60_000).toLong()).atZone(zone)
        return at(720 - 4 * (lon + ha) - eqTime) to at(720 - 4 * (lon - ha) - eqTime)
    }

    private fun dayWindow(date: LocalDate, zone: ZoneId, lat: Double?, lon: Double?): Pair<ZonedDateTime, ZonedDateTime> =
        (if (lat != null && lon != null) times(date, lat, lon, zone) else null)
            ?: (date.atTime(dawn).atZone(zone) to date.atTime(dusk).atZone(zone))

    fun isDay(now: ZonedDateTime, lat: Double?, lon: Double?): Boolean {
        val (rise, set) = dayWindow(now.toLocalDate(), now.zone, lat, lon)
        return !now.isBefore(rise) && now.isBefore(set)
    }

    fun nextChange(now: ZonedDateTime, lat: Double?, lon: Double?): ZonedDateTime {
        val (rise, set) = dayWindow(now.toLocalDate(), now.zone, lat, lon)
        val (tomorrowRise, _) = dayWindow(now.toLocalDate().plusDays(1), now.zone, lat, lon)
        return listOf(rise, set, tomorrowRise).first { it.isAfter(now) }
    }
}
