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
        assertTrue(samples.all { it.rpm!! in 700..5000 && it.speedKmh!! in 0..120 && it.batteryV!! in 13.5..14.6 })
        assertEquals(2, e.readDtcs()!!.count); assertTrue(e.clearDtcs()); assertEquals(0, e.readDtcs()!!.count)
    }
}
