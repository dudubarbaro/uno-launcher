package io.github.dudubarbaro.unolauncher.obd

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdEngineTest {
    private val car = mapOf(
        "ATZ" to "ELM327 v1.5", "ATE0" to "OK", "ATL0" to "OK", "ATS0" to "OK", "ATH0" to "OK", "ATSP0" to "OK",
        "0100" to "41 00 BE 3E A8 13", "ATDPN" to "A3", "010C" to "41 0C 1A F8", "010D" to "41 0D 3E", "0105" to "41 05 81",
        "ATRV" to "14.1V", "010B" to "41 0B 28", "010F" to "41 0F 46", "03" to "43 01 71 00 00 00 00", "07" to "NO DATA",
        "0A" to "NO DATA", "04" to "44",
    )

    @Test fun initThenPoll() = runTest {
        val f = FakeElm(car); val links = mutableListOf<Link>(); var last = Sample()
        val job = launch { ObdEngine { f.session() }.run({ links += it }, { last = it }) }
        advanceTimeBy(5_000); job.cancel()
        assertEquals(listOf("ATZ", "ATE0", "ATL0", "ATS0", "ATH0", "ATSP0", "0100", "ATDPN"), f.sent.take(8))
        assertEquals(Link.CONNECTED, links.last()); assertEquals(1726, last.rpm); assertEquals(62, last.speedKmh)
        assertEquals(89, last.coolantC); assertEquals(14.1, last.batteryV!!, 1e-9); assertEquals(40, last.mapKpa)
    }

    @Test fun pollPlanUsesSupported() {
        val e = ObdEngine { error("unused") }
        val mapCar = (0 until 8).flatMap { e.pollPlan(it, setOf(0x0B, 0x0F)) }.toSet()
        assertEquals(setOf("010C", "010D", "0105", "ATRV", "010B", "010F"), mapCar)
        val mafCar = (0 until 8).flatMap { e.pollPlan(it, setOf(0x10)) }.toSet()
        assertEquals(setOf("010C", "010D", "0105", "ATRV", "0110"), mafCar)
        assertEquals(listOf("010C", "010D"), e.pollPlan(0, emptySet()).take(2))
    }

    @Test fun dtcsAndClear() = runTest {
        val f = FakeElm(car); val e = ObdEngine { f.session() }
        val job = launch { e.run({}, {}) }; advanceTimeBy(1_000)
        assertEquals(Dtcs(listOf("P0171"), emptyList(), emptyList()), e.readDtcs())
        assertTrue(e.clearDtcs()); assertTrue("04" in f.sent); job.cancel()
    }

    @Test fun ignitionOffReportsIgnitionOff() = runTest {
        val f = FakeElm(car + ("0100" to "SEARCHING...\rUNABLE TO CONNECT")); val links = mutableListOf<Link>()
        val job = launch { ObdEngine { f.session() }.run({ links += it }, {}) }; advanceTimeBy(12_000); job.cancel()
        assertTrue(Link.IGNITION_OFF in links); assertFalse(Link.CONNECTED in links)
        assertTrue(f.sent.count { it == "0100" } >= 2) // continua tentando
    }

    @Test fun securityErrorRetries() = runTest {
        var opens = 0; val links = mutableListOf<Link>()
        val job = launch { ObdEngine { opens++; throw SecurityException("sem permissão") }.run({ links += it }, {}) }
        advanceTimeBy(10_000); job.cancel()
        assertTrue(opens >= 2); assertTrue(links.all { it == Link.CONNECTING })
    }

    @Test fun ecuSilentGoesIgnitionOff() = runTest {
        val script = car.toMutableMap(); val f = FakeElm(script); val links = mutableListOf<Link>()
        val job = launch { ObdEngine { f.session() }.run({ links += it }, {}) }
        advanceTimeBy(2_000)
        script["010C"] = "NO DATA"; script["010D"] = "NO DATA"; script["0100"] = "UNABLE TO CONNECT"
        advanceTimeBy(10_000); job.cancel()
        assertEquals(listOf(Link.CONNECTING, Link.CONNECTED), links.take(2)); assertEquals(Link.IGNITION_OFF, links.last())
    }

    @Test fun ioErrorReconnects() = runTest {
        var opens = 0; val links = mutableListOf<Link>()
        val job = launch { ObdEngine { opens++; FakeElm(car, failAfter = if (opens == 1) 12 else null).session() }.run({ links += it }, {}) }
        advanceTimeBy(10_000); job.cancel()
        assertTrue(opens >= 2); assertEquals(listOf(Link.CONNECTING, Link.CONNECTED, Link.CONNECTING, Link.CONNECTED), links.take(4))
    }
}
