package io.github.dudubarbaro.unolauncher.obd

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** Um comando por vez para o ELM327: escreve, lê até o prompt `>` e devolve o texto. */
class ElmSession(
    private val input: InputStream,
    private val output: OutputStream,
    private val onClose: () -> Unit = {},
) : Closeable {

    suspend fun cmd(c: String, timeoutMs: Long = 2000): String {
        while (input.available() > 0) input.read() // descarta resto de resposta anterior
        output.write("$c\r".toByteArray())
        output.flush()
        val sb = StringBuilder()
        try {
            withTimeout(timeoutMs) {
                var done = false
                while (!done) {
                    val n = input.available()
                    if (n == 0) { delay(10); continue }
                    repeat(n) {
                        if (done) return@repeat
                        val b = input.read()
                        if (b < 0) throw IOException("conexão fechada")
                        if (b == '>'.code) done = true else sb.append(b.toChar())
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw IOException("timeout $c")
        }
        return sb.toString().trim()
    }

    override fun close() = onClose()
}
