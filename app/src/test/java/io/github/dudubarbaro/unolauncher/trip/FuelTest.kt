package io.github.dudubarbaro.unolauncher.trip

import io.github.dudubarbaro.unolauncher.obd.Sample
import org.junit.Assert.assertEquals
import org.junit.Test

class FuelTest {
    @Test fun speedDensity() = assertEquals(7.3473, airGramsPerSecond(40.0, 30.0, 2400.0), 1e-3)

    @Test fun litersPerHourMap() {
        val s = Sample(rpm = 2400, mapKpa = 40, iatC = 30)
        assertEquals(2.6718, fuelLitersPerHour(s, Fuel.GASOLINA, 1.0), 1e-3)
        assertEquals(3.6328, fuelLitersPerHour(s, Fuel.ETANOL, 1.0), 1e-3)
        assertEquals(2.6718 * 1.1, fuelLitersPerHour(s, Fuel.GASOLINA, 1.1), 1e-3)
    }

    @Test fun mafWins() =
        assertEquals(2.7273, fuelLitersPerHour(Sample(rpm = 2400, mapKpa = 40, iatC = 30, mafGs = 7.5), Fuel.GASOLINA, 1.0), 1e-3)

    @Test fun engineOffOrMissing() {
        assertEquals(0.0, fuelLitersPerHour(Sample(rpm = 0, mapKpa = 100, iatC = 30), Fuel.GASOLINA, 1.0), 0.0)
        assertEquals(0.0, fuelLitersPerHour(Sample(rpm = 900), Fuel.GASOLINA, 1.0), 0.0)
    }
}
