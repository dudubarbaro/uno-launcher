package io.github.dudubarbaro.unolauncher.settings

import android.content.SharedPreferences
import io.github.dudubarbaro.unolauncher.trip.Fuel
import io.github.dudubarbaro.unolauncher.ui.Accent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepo(private val prefs: SharedPreferences) {
    private val state = MutableStateFlow(load())
    val flow: StateFlow<Settings> = state.asStateFlow()
    val value: Settings get() = state.value

    @Synchronized
    fun update(f: (Settings) -> Settings) {
        val next = f(state.value)
        if (next == state.value) return
        save(next)
        state.value = next
    }

    private fun d(k: String, def: Double) = prefs.getString(k, null)?.toDoubleOrNull() ?: def
    private fun dn(k: String) = prefs.getString(k, null)?.toDoubleOrNull()

    private fun load() = Settings().let { def ->
        Settings(
            shortcuts = prefs.getString("shortcuts", null)?.split('\n')?.takeIf { it.size == 3 } ?: def.shortcuts,
            fuel = Fuel.fromKey(prefs.getString("fuel", null)),
            pricePerL = d("pricePerL", def.pricePerL),
            fuelMultiplier = d("fuelMultiplier", def.fuelMultiplier),
            odometerKm = d("odometerKm", def.odometerKm),
            nextServiceKm = d("nextServiceKm", def.nextServiceKm),
            adapterAddress = prefs.getString("adapterAddress", null),
            startOpenApp = prefs.getString("startOpenApp", null),
            startResumeMusic = prefs.getBoolean("startResumeMusic", def.startResumeMusic),
            startObd = prefs.getBoolean("startObd", def.startObd),
            accent = Accent.fromKey(prefs.getString("accent", null)),
            autoBrightness = prefs.getBoolean("autoBrightness", def.autoBrightness),
            dayBrightness = prefs.getInt("dayBrightness", def.dayBrightness),
            nightBrightness = prefs.getInt("nightBrightness", def.nightBrightness),
            fullscreen = prefs.getBoolean("fullscreen", def.fullscreen),
            alertCoolantC = prefs.getInt("alertCoolantC", def.alertCoolantC),
            alertBatteryLowV = d("alertBatteryLowV", def.alertBatteryLowV),
            alertBatteryHighV = d("alertBatteryHighV", def.alertBatteryHighV),
            alertServiceBeforeKm = d("alertServiceBeforeKm", def.alertServiceBeforeKm),
            demo = prefs.getBoolean("demo", def.demo),
            lat = dn("lat"),
            lon = dn("lon"),
        )
    }

    private fun save(s: Settings) {
        val e = prefs.edit()
        fun str(k: String, v: String?) { if (v == null) e.remove(k) else e.putString(k, v) }
        str("shortcuts", s.shortcuts.joinToString("\n"))
        str("fuel", s.fuel.key)
        str("pricePerL", s.pricePerL.toString())
        str("fuelMultiplier", s.fuelMultiplier.toString())
        str("odometerKm", s.odometerKm.toString())
        str("nextServiceKm", s.nextServiceKm.toString())
        str("adapterAddress", s.adapterAddress)
        str("startOpenApp", s.startOpenApp)
        e.putBoolean("startResumeMusic", s.startResumeMusic)
        e.putBoolean("startObd", s.startObd)
        str("accent", s.accent.key)
        e.putBoolean("autoBrightness", s.autoBrightness)
        e.putInt("dayBrightness", s.dayBrightness)
        e.putInt("nightBrightness", s.nightBrightness)
        e.putBoolean("fullscreen", s.fullscreen)
        e.putInt("alertCoolantC", s.alertCoolantC)
        str("alertBatteryLowV", s.alertBatteryLowV.toString())
        str("alertBatteryHighV", s.alertBatteryHighV.toString())
        str("alertServiceBeforeKm", s.alertServiceBeforeKm.toString())
        e.putBoolean("demo", s.demo)
        str("lat", s.lat?.toString())
        str("lon", s.lon?.toString())
        e.apply()
    }
}
