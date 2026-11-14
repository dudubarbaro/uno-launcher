package io.github.dudubarbaro.unolauncher.media

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingTest {
    private val np = NowPlaying("t", "a", null, durationMs = 200_000, positionMs = 70_000, updatedAtMs = 1_000, speed = 1f, playing = true)

    @Test fun advancesWhilePlaying() = assertEquals(75_000L, np.positionAt(6_000))

    @Test fun frozenWhenPaused() = assertEquals(70_000L, np.copy(playing = false).positionAt(60_000))

    @Test fun cappedAtDuration() = assertEquals(200_000L, np.positionAt(1_000_000))
}
