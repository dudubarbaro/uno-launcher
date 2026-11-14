package io.github.dudubarbaro.unolauncher.sys

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EventLogTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun keepsLast50NewestFirst() {
        val log = EventLog(File(tmp.root, "events.txt"))
        repeat(60) { log.add("E$it", it * 1000L) }
        val r = log.recent(); assertEquals(50, r.size); assertTrue(r.first().endsWith("E59")); assertTrue(r.last().endsWith("E10"))
    }
}
