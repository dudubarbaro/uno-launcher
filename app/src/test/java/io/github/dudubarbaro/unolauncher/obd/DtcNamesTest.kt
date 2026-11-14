package io.github.dudubarbaro.unolauncher.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcNamesTest {
    @Test fun known() = assertTrue(DtcNames.describe("P0300").contains("falha de ignição", ignoreCase = true))

    @Test fun manufacturer() {
        assertEquals("Código do fabricante, confirme com um mecânico", DtcNames.describe("P1234"))
        assertEquals("Código do fabricante, confirme com um mecânico", DtcNames.describe("U3000"))
    }

    @Test fun genericUnknown() = assertEquals("Código genérico sem descrição no app", DtcNames.describe("P0999"))
}
