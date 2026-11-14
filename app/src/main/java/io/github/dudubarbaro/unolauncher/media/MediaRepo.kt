package io.github.dudubarbaro.unolauncher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val durationMs: Long,
    val positionMs: Long,
    /** Em `SystemClock.elapsedRealtime()`, como o `PlaybackState`. */
    val updatedAtMs: Long,
    val speed: Float,
    val playing: Boolean,
) {
    fun positionAt(nowElapsedMs: Long): Long {
        if (!playing) return positionMs
        val p = positionMs + ((nowElapsedMs - updatedAtMs) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p.coerceAtLeast(0)
    }
}

/** Música tocando em qualquer app (Spotify, rádio...), lida pelas sessões de mídia do sistema. */
object MediaRepo {
    private val state = MutableStateFlow<NowPlaying?>(null)
    val flow: StateFlow<NowPlaying?> = state.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = choose(null)
    }

    fun hasAccess(ctx: Context): Boolean = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun start(ctx: Context) {
        if (!hasAccess(ctx)) return
        val app = ctx.applicationContext
        val cn = ComponentName(app, MediaListener::class.java)
        runCatching {
            val m = app.getSystemService(MediaSessionManager::class.java)
            if (manager == null) {
                m.addOnActiveSessionsChangedListener({ choose(it) }, cn, main)
                manager = m
            }
            choose(m.getActiveSessions(cn))
        }
    }

    private fun choose(list: List<MediaController>?) {
        val pick = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list?.firstOrNull()
        if (pick?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = pick
            pick?.registerCallback(callback, main)
        }
        publish()
    }

    private fun publish() {
        val c = controller ?: run { state.value = null; return }
        val md = c.metadata
        val st = c.playbackState
        state.value = NowPlaying(
            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = (md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)).orEmpty(),
            art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0,
            positionMs = st?.position ?: 0,
            updatedAtMs = st?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
            playing = st?.state == PlaybackState.STATE_PLAYING,
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }

    fun prev() { controller?.transportControls?.skipToPrevious() }

    /** Retoma a música: pela sessão ativa, ou mandando "play" para o último app de música (normalmente o Spotify). */
    fun resume(ctx: Context) {
        controller?.let { it.transportControls.play(); return }
        val am = ctx.getSystemService(AudioManager::class.java) ?: return
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }
}
