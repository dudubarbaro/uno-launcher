package io.github.dudubarbaro.unolauncher.obd

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ElmSessionTest {
    @Test fun readsUntilPrompt() = runTest {
        val f = FakeElm(mapOf("ATRV" to "14.1V")); assertEquals("14.1V", f.session().cmd("ATRV"))
    }

    @Test fun timeoutThrows() {
        val silent = ElmSession(object : InputStream() { override fun read() = -1; override fun available() = 0 }, ByteArrayOutputStream())
        assertThrows(IOException::class.java) { runBlocking { silent.cmd("010C", 50) } }
    }
}
