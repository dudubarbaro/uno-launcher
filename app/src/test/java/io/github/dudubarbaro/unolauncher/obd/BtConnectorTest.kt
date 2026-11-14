package io.github.dudubarbaro.unolauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BtConnectorTest {
    private val bonded = listOf("JBL" to "AA:AA", "OBDII" to "BB:BB")

    @Test fun pickAdapter() {
        assertEquals("CC:CC", pickAdapter(bonded + ("X" to "CC:CC"), "CC:CC"))
        assertEquals("BB:BB", pickAdapter(bonded, "ZZ:ZZ"))
        assertEquals("BB:BB", pickAdapter(bonded, null))
        assertNull(pickAdapter(listOf("JBL" to "AA:AA"), null))
    }
}
