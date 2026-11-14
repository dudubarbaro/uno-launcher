package io.github.dudubarbaro.unolauncher.car

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.dudubarbaro.unolauncher.Graph
import io.github.dudubarbaro.unolauncher.trip.TripState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** Roda com o UnoApp real (Graph iniciado); sem permissão de Bluetooth concedida. */
@RunWith(RobolectricTestRunner::class)
class ObdServiceTest {
    private val app get() = ApplicationProvider.getApplicationContext<Application>()

    @Test fun tripSurvivesServiceStart() {
        val saved = TripState(1000, 12.5, 900.0, 1.1)
        Graph.trips.saveCurrent(saved)
        val c = Robolectric.buildService(ObdService::class.java).create()
        Thread.sleep(1_000) // o motor sobe em Dispatchers.IO
        assertEquals(saved, Graph.trips.loadCurrent())
        c.destroy()
        assertEquals(saved, Graph.trips.loadCurrent())
    }

    @Test fun demoStartsWithoutBluetoothPermission() {
        Graph.settings.update { it.copy(demo = true) }
        ObdService.start(app)
        assertNotNull(shadowOf(app).nextStartedService)
    }
}
