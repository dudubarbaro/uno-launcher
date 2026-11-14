package io.github.dudubarbaro.unolauncher.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Modifier
import io.github.dudubarbaro.unolauncher.car.Alert
import io.github.dudubarbaro.unolauncher.car.CarState
import io.github.dudubarbaro.unolauncher.media.NowPlaying
import io.github.dudubarbaro.unolauncher.obd.Dtcs
import io.github.dudubarbaro.unolauncher.obd.Link
import io.github.dudubarbaro.unolauncher.obd.Sample
import io.github.dudubarbaro.unolauncher.settings.Settings
import io.github.dudubarbaro.unolauncher.sys.Perm
import io.github.dudubarbaro.unolauncher.trip.Fuel
import io.github.dudubarbaro.unolauncher.trip.Trip
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

    private val shortcuts = listOf(AppEntry("Waze", "com.waze", null), AppEntry("Spotify", "com.spotify.music", null), AppEntry("Consis", "", null))

    @Test fun home() = shoot("home") {
        HomeScreen(HomeUi("14:32", "sáb 14 nov", "27° ☀", "89°C", "14,1 V", null, music, true, null, false, shortcuts), {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun homeLightsOn() = shoot("home-luzes") {
        HomeScreen(HomeUi("14:32", "sáb 14 nov", "27° ☀", "89°C", "14,1 V", null, music, true, null, true, shortcuts), {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun homeNoObdNoMusic() = shoot("home-sem-obd") {
        HomeScreen(HomeUi("14:32", "sáb 14 nov", null, "—", "—", "OBD2 desconectado", null, false, null, false, shortcuts), {}, {}, {}, {}, {}, {}, {})
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

    @Test fun dtcs() = shoot("dtc") { DtcScreen(Dtcs(listOf("P0171"), listOf("P0300"), emptyList()), Link.CONNECTED, false, {}, {}, {}) }

    @Test fun dtcsEmpty() = shoot("dtc-vazio") { DtcScreen(Dtcs(emptyList(), emptyList(), emptyList()), Link.CONNECTED, false, {}, {}, {}) }

    @Test fun dtcsDisconnected() = shoot("dtc-desconectado") { DtcScreen(null, Link.OFF, false, {}, {}, {}) }

    @Test fun trips() = shoot("viagens") {
        val day = 1_794_600_000_000L
        TripsScreen(
            TripState(0, 12.0, 1500.0, 1.6),
            listOf(
                Trip(day, day + 2_400_000, 23.4, 2400.0, 2.9, Fuel.ETANOL, 2.9 * 4.19),
                Trip(day - 86_400_000, day - 86_400_000 + 900_000, 6.1, 900.0, 0.8, Fuel.ETANOL, 0.8 * 4.19),
            ),
            etanol, {}, {},
        )
    }

    @Test fun settings() = shoot("config") {
        SettingsScreen(
            Settings(), setOf(Perm.BLUETOOTH), listOf("OBDII" to "BB:BB"), null, listOf("Waze", "Spotify", "Consis"), true,
            rememberLazyListState(), {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test fun apps() = shoot("apps") {
        val names = listOf("Waze", "Spotify", "YouTube Music", "Chrome", "Arquivos", "Rádio", "Maps", "Telefone", "Configurações", "Câmera", "Galeria", "Uno Launcher")
        AppsScreen("Apps", names.map { AppEntry(it, "pkg.$it", null) }, {}, {})
    }

    @Test fun about() = shoot("sobre") {
        AboutScreen(
            listOf("Resolução" to "1024×600", "Densidade" to "160 dpi", "Android" to "13 (SDK 33)"),
            listOf("2026-11-14 07:58:02  SCREEN_ON", "2026-11-14 07:58:01  APP INICIADO (boot #42)", "2026-11-14 07:57:55  BOOT_COMPLETED"),
            {},
        )
    }
}
