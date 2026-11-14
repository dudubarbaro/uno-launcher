package io.github.dudubarbaro.unolauncher.env

import android.content.SharedPreferences
import io.github.dudubarbaro.unolauncher.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Weather(val tempC: Double, val symbol: String, val atMs: Long)

/** Previsão do met.no (Instituto Meteorológico da Noruega), gratuita, exige User-Agent com contato. */
object Metno {
    fun url(lat: Double, lon: Double): String =
        String.format(Locale.ROOT, "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.2f&lon=%.2f", lat, lon)

    fun parse(json: String, nowMs: Long): Weather? = runCatching {
        val data = JSONObject(json).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val temp = data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")
        val next = data.optJSONObject("next_1_hours") ?: data.getJSONObject("next_6_hours")
        Weather(temp, next.getJSONObject("summary").getString("symbol_code"), nowMs)
    }.getOrNull()

    fun emoji(symbol: String): String = when {
        "thunder" in symbol -> "⛈"
        "snow" in symbol -> "❄"
        "rain" in symbol || "sleet" in symbol -> "🌧"
        "fog" in symbol -> "🌫"
        "fair" in symbol || "partlycloudy" in symbol -> "⛅"
        "cloudy" in symbol -> "☁"
        symbol.startsWith("clearsky_night") -> "☾"
        symbol.startsWith("clearsky") -> "☀"
        else -> ""
    }

    fun userAgent(): String = "UnoLauncher/${BuildConfig.VERSION_NAME} ${BuildConfig.MET_CONTACT}".trim()
}

/** Última previsão guardada; busca de novo no máximo a cada 30 min. */
class WeatherRepo(private val prefs: SharedPreferences) {
    private val state = MutableStateFlow(load())
    val flow: StateFlow<Weather?> = state.asStateFlow()

    private fun load(): Weather? {
        val sym = prefs.getString("w_symbol", null) ?: return null
        return Weather(prefs.getFloat("w_temp", 0f).toDouble(), sym, prefs.getLong("w_at", 0))
    }

    suspend fun refreshIfStale(lat: Double, lon: Double, nowMs: Long) {
        if (nowMs - prefs.getLong("w_fetch", 0) < STALE_MS) return
        prefs.edit().putLong("w_fetch", nowMs).apply() // conta a tentativa, mesmo se falhar
        val w = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(Metno.url(lat, lon)).openConnection() as HttpURLConnection
                c.setRequestProperty("User-Agent", Metno.userAgent())
                c.connectTimeout = 10_000; c.readTimeout = 10_000
                try {
                    if (c.responseCode == 200) Metno.parse(c.inputStream.bufferedReader().readText(), nowMs) else null
                } finally {
                    c.disconnect()
                }
            }.getOrNull()
        } ?: return
        prefs.edit().putFloat("w_temp", w.tempC.toFloat()).putString("w_symbol", w.symbol).putLong("w_at", w.atMs).apply()
        state.value = w
    }

    private companion object { const val STALE_MS = 30 * 60_000L }
}
