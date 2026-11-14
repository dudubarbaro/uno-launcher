package io.github.dudubarbaro.unolauncher

import android.app.Application
import io.github.dudubarbaro.unolauncher.env.WeatherRepo
import io.github.dudubarbaro.unolauncher.settings.SettingsRepo
import io.github.dudubarbaro.unolauncher.sys.EventLog
import io.github.dudubarbaro.unolauncher.trip.TripStore
import java.io.File

/** Instâncias únicas do app (app pessoal, um processo só). */
object Graph {
    lateinit var settings: SettingsRepo
    lateinit var trips: TripStore
    lateinit var events: EventLog
    lateinit var weather: WeatherRepo
}

class UnoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.settings = SettingsRepo(getSharedPreferences("settings", MODE_PRIVATE))
        Graph.trips = TripStore(File(filesDir, "trips"))
        Graph.events = EventLog(File(filesDir, "events.txt"))
        Graph.weather = WeatherRepo(getSharedPreferences("weather", MODE_PRIVATE))
    }
}
