package io.github.dudubarbaro.unolauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdTest {
    @Test fun liveValues() {
        assertEquals(1726, Obd.rpm("41 0C 1A F8\r\r>"))
        assertEquals(1000, Obd.rpm("SEARCHING...\r41 0C 0F A0\r"))
        assertEquals(1726, Obd.rpm("010C\r410C1AF8\r")) // eco ligado, sem espaços
        assertEquals(62, Obd.speedKmh("41 0D 3E"))
        assertEquals(89, Obd.coolantC("41 05 81"))
        assertEquals(40, Obd.mapKpa("41 0B 28"))
        assertEquals(30, Obd.iatC("41 0F 46"))
        assertEquals(7.5, Obd.mafGs("41 10 02 EE")!!, 1e-9)
        assertEquals(14.1, Obd.voltage("14.1V")!!, 1e-9)
    }

    @Test fun noAnswer() {
        assertNull(Obd.rpm("NO DATA"))
        assertNull(Obd.speedKmh("UNABLE TO CONNECT"))
        assertNull(Obd.voltage("?"))
        assertTrue(Obd.unableToConnect("SEARCHING...\rUNABLE TO CONNECT"))
        assertTrue(Obd.unableToConnect("BUS INIT: ...ERROR"))
        assertFalse(Obd.unableToConnect("41 0C 1A F8"))
    }

    @Test fun supported() {
        val s = Obd.supportedPids("41 00 BE 1F A8 13")
        assertTrue(s.containsAll(listOf(0x01, 0x05, 0x0C, 0x0D, 0x0F, 0x10)))
        assertFalse(0x0B in s); assertFalse(0x02 in s)
    }

    @Test fun protocol() {
        assertTrue(Obd.isCan("A6")); assertTrue(Obd.isCan("6"))
        assertFalse(Obd.isCan("3")); assertFalse(Obd.isCan("A5"))
    }

    @Test fun dtcNonCan() {
        assertEquals(listOf("P0133"), Obd.dtcs("43 01 33 00 00 00 00", 0x43, false))
        assertEquals(
            listOf("P0171", "P0300", "P0133", "P0420"),
            Obd.dtcs("43 01 71 03 00 01 33\r43 04 20 00 00 00 00", 0x43, false),
        )
    }

    @Test fun dtcCan() {
        assertEquals(listOf("P0171", "P0300"), Obd.dtcs("43 02 01 71 03 00", 0x43, true))
        assertEquals(
            listOf("P0171", "P0300", "P0133", "P0420", "U0123"),
            Obd.dtcs("00C\r0:430501710300\r1:01330420C12300\r\r>", 0x43, true),
        )
        assertEquals(emptyList<String>(), Obd.dtcs("47 00", 0x47, true))
        assertEquals(emptyList<String>(), Obd.dtcs("NO DATA", 0x4A, true))
    }

    @Test fun decode() {
        assertEquals("P0133", Obd.decodeDtc(0x01, 0x33)); assertEquals("C0123", Obd.decodeDtc(0x41, 0x23))
        assertEquals("B0123", Obd.decodeDtc(0x81, 0x23)); assertEquals("U0123", Obd.decodeDtc(0xC1, 0x23))
        assertEquals("P1234", Obd.decodeDtc(0x12, 0x34))
    }
}
