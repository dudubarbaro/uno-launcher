package io.github.dudubarbaro.unolauncher.settings

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dudubarbaro.unolauncher.trip.Fuel
import io.github.dudubarbaro.unolauncher.ui.Accent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SettingsRepoTest {
    @Test fun roundTrip() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("t", 0)
        val changed = Settings(
            fuel = Fuel.ETANOL, pricePerL = 4.19, adapterAddress = "00:11:22:33:44:55", startOpenApp = "com.waze",
            accent = Accent.AZUL, fullscreen = true, alertCoolantC = 110, demo = true, lat = -22.91, lon = -43.17, odometerKm = 98765.4,
            shortcuts = listOf("com.a", "com.b", "com.c"),
        )
        SettingsRepo(prefs).update { changed }
        assertEquals(changed, SettingsRepo(prefs).value)
    }

    @Test fun badShortcutsFallBackToDefault() {
        val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("t2", 0)
        prefs.edit().putString("shortcuts", "com.a\ncom.b").commit()
        assertEquals(Settings().shortcuts, SettingsRepo(prefs).value.shortcuts)
    }

    @Test fun defaultShortcuts() =
        assertEquals(listOf("com.waze", "com.spotify.music", CONSIS_BY_NAME), Settings().shortcuts)
}
