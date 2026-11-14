package io.github.dudubarbaro.unolauncher.sys

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.dudubarbaro.unolauncher.Graph
import io.github.dudubarbaro.unolauncher.env.Brightness

/** Registra o boot (normal ou "quickboot" das centrais chinesas) e acerta o brilho. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Graph.events.add(intent.action ?: "BOOT?", System.currentTimeMillis())
        Brightness.apply(context)
    }
}
