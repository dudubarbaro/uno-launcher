package io.github.dudubarbaro.unolauncher.env

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetnoTest {
    private val json = """{"properties":{"timeseries":[{"time":"2026-11-14T15:00:00Z","data":{"instant":{"details":{"air_temperature":27.3}},
        "next_1_hours":{"summary":{"symbol_code":"clearsky_day"}}}}]}}"""

    @Test fun parses() = assertEquals(Weather(27.3, "clearsky_day", 5L), Metno.parse(json, 5L))

    @Test fun sixHourFallback() =
        assertEquals("rain", Metno.parse(json.replace("next_1_hours", "next_6_hours").replace("clearsky_day", "rain"), 0)!!.symbol)

    @Test fun broken() = assertNull(Metno.parse("{}", 0))

    @Test fun emoji() {
        assertEquals("☀", Metno.emoji("clearsky_day")); assertEquals("🌧", Metno.emoji("lightrainshowers_night"))
        assertEquals("⛈", Metno.emoji("heavyrainandthunder"))
    }

    @Test fun url() =
        assertEquals("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=-22.91&lon=-43.17", Metno.url(-22.9068, -43.1729))
}
