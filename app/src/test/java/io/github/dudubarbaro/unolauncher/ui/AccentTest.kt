package io.github.dudubarbaro.unolauncher.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AccentTest {
    @Test fun fromKeyKnown() = assertEquals(Accent.AZUL, Accent.fromKey("azul"))

    @Test fun fromKeyUnknownIsRed() {
        assertEquals(Accent.VERMELHO, Accent.fromKey("roxo"))
        assertEquals(Accent.VERMELHO, Accent.fromKey(null))
    }
}
