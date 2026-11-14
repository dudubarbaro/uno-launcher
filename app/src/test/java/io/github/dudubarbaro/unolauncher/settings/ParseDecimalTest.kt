package io.github.dudubarbaro.unolauncher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseDecimalTest {
    @Test fun parses() {
        assertEquals(5.89, parseDecimal("5,89")!!, 1e-9); assertEquals(5.89, parseDecimal(" 5.89 ")!!, 1e-9)
        assertEquals(12.0, parseDecimal("12")!!, 1e-9)
        assertNull(parseDecimal("")); assertNull(parseDecimal("abc")); assertNull(parseDecimal("-3")); assertNull(parseDecimal("1,2,3"))
    }
}
