package io.github.dudubarbaro.unolauncher.env

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import io.github.dudubarbaro.unolauncher.Graph
import kotlin.math.roundToLong

/** Guarda a última localização conhecida, arredondada a 2 casas (~1 km), para clima e brilho. */
@SuppressLint("MissingPermission") // checado logo abaixo
fun refreshLocation(ctx: Context) {
    if (ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    val lm = ctx.getSystemService(LocationManager::class.java) ?: return
    val best = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time } ?: return
    fun r2(x: Double) = (x * 100).roundToLong() / 100.0
    Graph.settings.update { it.copy(lat = r2(best.latitude), lon = r2(best.longitude)) }
}
