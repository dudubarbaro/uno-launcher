package io.github.dudubarbaro.unolauncher.env

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings.System as Sys
import io.github.dudubarbaro.unolauncher.Graph
import java.time.ZonedDateTime

/** Brilho de dia ou de noite pelo nascer e pôr do sol. Precisa de "alterar configurações do sistema". */
object Brightness {
    fun canWrite(ctx: Context): Boolean = Sys.canWrite(ctx)

    fun apply(ctx: Context) {
        val s = Graph.settings.value
        if (!s.autoBrightness || !canWrite(ctx)) return
        val now = ZonedDateTime.now()
        val value = if (Sun.isDay(now, s.lat, s.lon)) s.dayBrightness else s.nightBrightness
        runCatching {
            Sys.putInt(ctx.contentResolver, Sys.SCREEN_BRIGHTNESS_MODE, Sys.SCREEN_BRIGHTNESS_MODE_MANUAL)
            Sys.putInt(ctx.contentResolver, Sys.SCREEN_BRIGHTNESS, value.coerceIn(1, 255))
        }
        // Alarme inexato: não precisa de permissão de alarme exato; alguns minutos de atraso não importam.
        val pi = PendingIntent.getBroadcast(
            ctx, 0, Intent(ctx, BrightnessReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val next = Sun.nextChange(now, s.lat, s.lon).toInstant().toEpochMilli() + 30_000
        ctx.getSystemService(AlarmManager::class.java)?.set(AlarmManager.RTC, next, pi)
    }
}

class BrightnessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Brightness.apply(context)
}
