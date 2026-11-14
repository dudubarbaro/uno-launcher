package io.github.dudubarbaro.unolauncher.media

import android.service.notification.NotificationListenerService

/** Existe só para o sistema liberar o acesso às sessões de mídia. */
class MediaListener : NotificationListenerService() {
    override fun onListenerConnected() = MediaRepo.start(this)
}
