package io.github.dudubarbaro.unolauncher.sys

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.github.dudubarbaro.unolauncher.env.Brightness
import io.github.dudubarbaro.unolauncher.media.MediaRepo
import io.github.dudubarbaro.unolauncher.obd.hasBluetooth

enum class Perm(val label: String) {
    BLUETOOTH("Bluetooth"),
    NOTIFICACOES("Notificações"),
    MUSICA("Acesso à música"),
    BRILHO("Alterar brilho"),
    LOCALIZACAO("Localização"),
    TELA_INICIAL("Tela inicial padrão"),
}

object Perms {
    private fun has(ctx: Context, p: String) = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    fun granted(ctx: Context): Set<Perm> = buildSet {
        if (hasBluetooth(ctx)) add(Perm.BLUETOOTH)
        if (Build.VERSION.SDK_INT < 33 || has(ctx, Manifest.permission.POST_NOTIFICATIONS)) add(Perm.NOTIFICACOES)
        if (MediaRepo.hasAccess(ctx)) add(Perm.MUSICA)
        if (Brightness.canWrite(ctx)) add(Perm.BRILHO)
        if (has(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)) add(Perm.LOCALIZACAO)
        if (isDefaultHome(ctx)) add(Perm.TELA_INICIAL)
    }

    /** Permissões pedidas na hora; vazio = vai pela tela de configurações do sistema. */
    fun runtime(p: Perm): Array<String> = when (p) {
        Perm.BLUETOOTH -> if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()
        Perm.NOTIFICACOES -> if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
        Perm.LOCALIZACAO -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        else -> emptyArray()
    }

    fun settingsIntent(ctx: Context, p: Perm): Intent? = when (p) {
        Perm.MUSICA -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        Perm.BRILHO -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
        Perm.TELA_INICIAL -> Intent(Settings.ACTION_HOME_SETTINGS)
        else -> null
    }

    fun isDefaultHome(ctx: Context): Boolean =
        ctx.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == ctx.packageName
}
