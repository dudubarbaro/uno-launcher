package io.github.dudubarbaro.unolauncher.sys

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings.Global
import android.widget.Toast
import io.github.dudubarbaro.unolauncher.Graph
import io.github.dudubarbaro.unolauncher.car.ObdService
import io.github.dudubarbaro.unolauncher.media.MediaRepo

object Startup {
    /** Número do boot atual; usa a hora do boot se a central não informar o contador. */
    fun bootId(ctx: Context): Int =
        runCatching { Global.getInt(ctx.contentResolver, Global.BOOT_COUNT) }
            .getOrElse { ((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 60_000).toInt() }

    /**
     * Ações "ao ligar o carro", uma vez por boot. Roda pela tela (e não pelo receiver de boot)
     * porque o Android não deixa um receiver abrir outro app.
     */
    // ponytail: dispara por boot; se a central só dormir/acordar, ligar ao SCREEN_ON depois do teste no carro
    fun runIfNewBoot(activity: Activity) {
        val boot = bootId(activity)
        val prefs = activity.getSharedPreferences("startup", Context.MODE_PRIVATE)
        val last = prefs.getInt("lastBoot", Int.MIN_VALUE)
        if (last == boot) return
        prefs.edit().putInt("lastBoot", boot).apply()
        if (last == Int.MIN_VALUE) return // primeira abertura depois de instalar não é "ligar o carro"
        val s = Graph.settings.value
        if (s.startObd || s.demo) ObdService.start(activity)
        if (s.startResumeMusic) {
            val app = activity.applicationContext
            Handler(Looper.getMainLooper()).postDelayed({ MediaRepo.resume(app) }, 3_000)
        }
        s.startOpenApp?.let { launch(activity, it) }
    }

    fun launch(ctx: Context, pkg: String, label: String = pkg): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        if (i == null) {
            Toast.makeText(ctx, "$label não está instalado", Toast.LENGTH_SHORT).show()
            return false
        }
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
