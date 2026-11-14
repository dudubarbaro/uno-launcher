package io.github.dudubarbaro.unolauncher.car

import io.github.dudubarbaro.unolauncher.obd.Sample
import io.github.dudubarbaro.unolauncher.settings.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertsTest {
    private val cfg = Settings()

    @Test fun coolant() {
        assertEquals(setOf(Alert.MOTOR_QUENTE), alerts(Sample(coolantC = 106), cfg))
        assertTrue(alerts(Sample(coolantC = 105), cfg).isEmpty())
    }

    @Test fun battery() {
        assertEquals(setOf(Alert.BATERIA_BAIXA), alerts(Sample(rpm = 800, batteryV = 11.9), cfg))
        assertTrue(alerts(Sample(rpm = 0, batteryV = 11.9), cfg).isEmpty())
        assertEquals(setOf(Alert.BATERIA_ALTA), alerts(Sample(batteryV = 15.1), cfg))
    }

    @Test fun service() {
        assertEquals(setOf(Alert.REVISAO), alerts(Sample(), cfg.copy(odometerKm = 9600.0, nextServiceKm = 10000.0)))
        assertTrue(alerts(Sample(), cfg.copy(odometerKm = 9400.0, nextServiceKm = 10000.0)).isEmpty())
        assertTrue(alerts(Sample(), cfg.copy(odometerKm = 9600.0, nextServiceKm = 0.0)).isEmpty())
    }
}
