package io.github.dudubarbaro.unolauncher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.dudubarbaro.unolauncher.env.WeatherRepo
import io.github.dudubarbaro.unolauncher.settings.SettingsRepo
import io.github.dudubarbaro.unolauncher.sys.EventLog
import io.github.dudubarbaro.unolauncher.sys.Startup
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

        Graph.events.add("APP INICIADO (boot #${Startup.bootId(this)})", System.currentTimeMillis())
        // Eventos que podem indicar ignição na central; só chegam com o app vivo (é a tela inicial, quase sempre está).
        val filter = IntentFilter().apply {
            listOf(
                Intent.ACTION_SCREEN_ON, Intent.ACTION_SCREEN_OFF, Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED, Intent.ACTION_SHUTDOWN,
            ).forEach(::addAction)
        }
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    Graph.events.add(intent.action?.substringAfterLast('.') ?: "?", System.currentTimeMillis())
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
