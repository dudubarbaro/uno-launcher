package io.github.dudubarbaro.unolauncher.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import io.github.dudubarbaro.unolauncher.car.Alert
import io.github.dudubarbaro.unolauncher.car.CarState
import io.github.dudubarbaro.unolauncher.media.NowPlaying
import io.github.dudubarbaro.unolauncher.obd.Dtcs
import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.obd.Sample
import io.github.dudubarbaro.unolauncher.settings.Settings
import io.github.dudubarbaro.unolauncher.trip.Fuel
import io.github.dudubarbaro.unolauncher.trip.TripState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class)
class ScreensShotTest {
    private val music = NowPlaying("Nome da música", "Artista", null, 200_000, 70_000, 0, 1f, true)
    private val etanol = Settings(fuel = Fuel.ETANOL, pricePerL = 4.19)
    private val connected = CarState(
        Link.CONNECTED, Sample(2400, 62, 89, 14.1, 40, 30), Dtcs(emptyList(), emptyList(), emptyList()),
        TripState(0, 12.0, 1500.0, 1.6), emptySet(),
    )

    @Test fun home() = shoot("home") {
        HomeScreen(HomeUi("14:32", "sáb 14 nov", "27° ☀", "89°C", "14,1 V", null, music, true, null), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun homeNoObdNoMusic() = shoot("home-sem-obd") {
        HomeScreen(HomeUi("14:32", "sáb 14 nov", null, "—", "—", "OBD2 desconectado", null, false, null), {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun obd() = shoot("obd") { ObdScreen(connected, etanol, null, {}, {}, {}, {}) }

    @Test fun obdIgnitionOff() = shoot("obd-ignicao") { ObdScreen(CarState(Link.IGNITION_OFF), etanol, null, {}, {}, {}, {}) }

    @Test fun obdAlert() = shoot("obd-alerta") {
        val hot = connected.copy(sample = connected.sample.copy(coolantC = 107), alerts = setOf(Alert.MOTOR_QUENTE))
        Column {
            AlertBanner(hot.alerts)
            Box(Modifier.weight(1f)) { ObdScreen(hot, etanol, null, {}, {}, {}, {}) }
        }
    }
}
