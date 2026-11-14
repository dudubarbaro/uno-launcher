package io.github.dudubarbaro.unolauncher.env

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SunTest {
    private val rio = ZoneId.of("America/Sao_Paulo"); private val lat = -22.9068; private val lon = -43.1729
    private fun near(h: Int, m: Int, t: ZonedDateTime) = assertTrue("$t", abs(t.hour * 60 + t.minute - (h * 60 + m)) <= 5)

    @Test fun rioNovember() { val (r, s) = Sun.times(LocalDate.of(2026, 11, 14), lat, lon, rio)!!; near(5, 1, r); near(18, 12, s) }

    @Test fun rioJune() { val (r, s) = Sun.times(LocalDate.of(2026, 6, 21), lat, lon, rio)!!; near(6, 32, r); near(17, 15, s) }

    @Test fun dayNight() {
        assertTrue(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, rio), lat, lon))
        assertFalse(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 23, 0, 0, 0, rio), lat, lon))
        assertFalse(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 5, 59, 0, 0, rio), null, null))
        assertTrue(Sun.isDay(ZonedDateTime.of(2026, 11, 14, 6, 0, 0, 0, rio), null, null))
    }

    @Test fun next() {
        near(18, 12, Sun.nextChange(ZonedDateTime.of(2026, 11, 14, 12, 0, 0, 0, rio), lat, lon))
        val n = Sun.nextChange(ZonedDateTime.of(2026, 11, 14, 20, 0, 0, 0, rio), lat, lon)
        assertEquals(15, n.dayOfMonth); near(5, 1, n)
    }
}
