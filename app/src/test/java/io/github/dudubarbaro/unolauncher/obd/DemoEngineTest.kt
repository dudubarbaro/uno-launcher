package io.github.dudubarbaro.unolauncher.obd

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoEngineTest {
    @Test fun plausibleAndClearable() = runTest {
        val e = DemoEngine(); val samples = mutableListOf<Sample>(); var link: Link? = null
        val job = launch { e.run({ link = it }, { samples += it }) }; advanceTimeBy(60_000); job.cancel()
        assertEquals(Link.DEMO, link); assertTrue(samples.size > 100)
        val running = samples.drop(DemoEngine.HALF_KEY_MS.toInt() / 250)
        assertTrue(running.all { it.rpm!! in 700..5000 && it.speedKmh!! in 0..120 && it.batteryV!! in 13.5..14.6 })
        assertEquals(2, e.readDtcs()!!.count); assertTrue(e.clearDtcs()); assertEquals(0, e.readDtcs()!!.count)
    }

    /** Começa em meia chave para mostrar as luzes acendendo na partida. */
    @Test fun startsAtHalfKey() = runTest {
        val e = DemoEngine(); val samples = mutableListOf<Sample>()
        val job = launch { e.run({}, { samples += it }) }; advanceTimeBy(10_000); job.cancel()
        val halfKey = samples.take(DemoEngine.HALF_KEY_MS.toInt() / 250)
        assertTrue(halfKey.isNotEmpty() && halfKey.all { it.rpm == 0 && it.speedKmh == 0 })
        assertTrue(samples.last().rpm!! > 0)
    }
}
