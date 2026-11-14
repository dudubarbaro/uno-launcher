package io.github.dudubarbaro.unolauncher.trip

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TripStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun currentAndFinish() {
        val st = TripStore(tmp.root)
        assertNull(st.loadCurrent())
        st.saveCurrent(TripState(1000, 12.5, 900.0, 1.1))
        assertEquals(TripState(1000, 12.5, 900.0, 1.1), TripStore(tmp.root).loadCurrent())
        st.finish(TripState(1000, 12.5, 900.0, 1.1), 2000, Fuel.ETANOL, 4.0)
        assertNull(st.loadCurrent())
        assertEquals(listOf(Trip(1000, 2000, 12.5, 900.0, 1.1, Fuel.ETANOL, 4.4)), st.trips())
    }

    @Test fun tinyTripNotSaved() {
        val st = TripStore(tmp.root); st.finish(TripState(1, 0.05, 10.0, 0.0), 2, Fuel.GASOLINA, 5.0)
        assertTrue(st.trips().isEmpty())
    }

    @Test fun corruptLineSkipped() {
        val st = TripStore(tmp.root); st.finish(TripState(1, 3.0, 60.0, 0.2), 2, Fuel.GASOLINA, 5.0)
        File(tmp.root, "trips.csv").appendText("1;2;lixo\n")
        assertEquals(1, st.trips().size)
    }

    @Test fun currentSurvivesPartialTmp() {
        val st = TripStore(tmp.root); st.saveCurrent(TripState(1, 2.0, 3.0, 0.1))
        File(tmp.root, "current.tmp").writeText("1;2") // corte de energia no meio da escrita
        assertEquals(TripState(1, 2.0, 3.0, 0.1), TripStore(tmp.root).loadCurrent())
    }

    @Test fun newestFirst() {
        val st = TripStore(tmp.root)
        st.finish(TripState(1, 1.0, 1.0, 0.1), 10, Fuel.GASOLINA, 1.0); st.finish(TripState(20, 1.0, 1.0, 0.1), 30, Fuel.GASOLINA, 1.0)
        assertEquals(listOf(20L, 1L), st.trips().map { it.startMs })
    }
}
