package io.github.dudubarbaro.unolauncher.sys

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Últimos 50 eventos do sistema (boot, energia, tela), para descobrir como a central avisa a ignição. */
class EventLog(private val file: File) {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)

    @Synchronized
    fun add(event: String, nowMs: Long) {
        val lines = (if (file.exists()) file.readLines() else emptyList()) + "${fmt.format(Date(nowMs))}  $event"
        file.parentFile?.mkdirs()
        file.writeText(lines.takeLast(MAX).joinToString("\n", postfix = "\n"))
    }

    @Synchronized
    fun recent(): List<String> = if (file.exists()) file.readLines().filter { it.isNotBlank() }.reversed() else emptyList()

    private companion object { const val MAX = 50 }
}
