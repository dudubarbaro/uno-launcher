package io.github.dudubarbaro.unolauncher.trip

import io.github.dudubarbaro.unolauncher.obd.Sample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripMeterTest {
    private val s = Sample(rpm = 2400, speedKmh = 60, mapKpa = 40, iatC = 30)

    @Test fun integrates() {
        var t = TripState()
        repeat(60) { t = advance(t, s, 1.0, 1000L * (it + 1), Fuel.GASOLINA, 1.0) }
        assertEquals(1.0, t.km, 1e-9); assertEquals(60.0, t.seconds, 1e-9)
        assertEquals(2.6718 / 60, t.liters, 1e-4); assertEquals(1000L, t.startMs)
        assertEquals(22.457, t.kmPerL()!!, 0.05); assertEquals(t.liters * 5.89, t.cost(5.89), 1e-9)
    }

    @Test fun gapIsIgnored() {
        val t = advance(TripState(km = 1.0), s, 10.0, 0, Fuel.GASOLINA, 1.0)
        assertEquals(1.0, t.km, 0.0); assertEquals(0.0, t.liters, 0.0)
    }

    @Test fun noLitersNoKmPerL() = assertNull(TripState(km = 1.0).kmPerL())
}
