package io.github.dudubarbaro.unolauncher.ui

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FmtTest {
    @Test fun ptBr() {
        assertEquals("89°C", Fmt.temp(89)); assertEquals("—", Fmt.temp(null))
        assertEquals("14,1 V", Fmt.volts(14.08)); assertEquals("—", Fmt.volts(null))
        assertEquals("R$ 12,40", Fmt.money(12.4)); assertEquals("7,8 km/l", Fmt.kmPerL(7.81)); assertEquals("— km/l", Fmt.kmPerL(null))
        assertEquals("12,5 km", Fmt.km(12.46)); assertEquals("1h05", Fmt.duration(3900.0)); assertEquals("15 min", Fmt.duration(900.0))
        val t = ZonedDateTime.of(2026, 11, 14, 14, 32, 0, 0, ZoneId.of("America/Sao_Paulo"))
        assertEquals("14:32", Fmt.clock(t)); assertEquals("sáb 14 nov", Fmt.date(t))
    }
}
