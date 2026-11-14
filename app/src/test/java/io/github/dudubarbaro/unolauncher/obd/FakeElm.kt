package io.github.dudubarbaro.unolauncher.obd

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** ELM327 de mentira: cada comando terminado em \r recebe a resposta do [script] seguida do prompt. */
class FakeElm(private val script: Map<String, String>, private val failAfter: Int? = null) {
    val sent = mutableListOf<String>()
    private val pending = ArrayDeque<Int>()
    private val buf = StringBuilder()

    private val input = object : InputStream() {
        override fun read(): Int = pending.removeFirstOrNull() ?: -1
        override fun available(): Int = pending.size
    }

    private val output = object : OutputStream() {
        override fun write(b: Int) {
            val c = b.toChar()
            if (c != '\r') { buf.append(c); return }
            val cmd = buf.toString(); buf.clear()
            if (failAfter != null && sent.size >= failAfter) throw IOException("link caiu")
            sent += cmd
            "${script[cmd] ?: "?"}\r\r>".forEach { pending.addLast(it.code) }
        }
    }

    fun session() = ElmSession(input, output)
}
