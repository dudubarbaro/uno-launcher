package io.github.dudubarbaro.unolauncher.car

import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.obd.Sample
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineRunningTest {
    private fun at(link: Link, rpm: Int?) = CarState(link = link, sample = Sample(rpm = rpm)).engineRunning

    @Test fun halfKeyIsOff() = assertEquals(false, at(Link.CONNECTED, 0))
    @Test fun noRpmIsOff() = assertEquals(false, at(Link.CONNECTED, null))
    @Test fun runningIsOn() = assertEquals(true, at(Link.CONNECTED, 850))
    @Test fun demoRunningIsOn() = assertEquals(true, at(Link.DEMO, 850))
    @Test fun staleRpmWhenNotConnectedIsOff() =
        listOf(Link.OFF, Link.CONNECTING, Link.IGNITION_OFF).forEach { assertEquals(it.name, false, at(it, 850)) }
}
