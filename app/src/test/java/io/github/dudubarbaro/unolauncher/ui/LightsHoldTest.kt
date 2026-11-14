package io.github.dudubarbaro.unolauncher.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Uma leitura de rotação perdida não pode piscar os faróis; desligar o motor apaga depois de um instante. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1024dp-h600dp-land-mdpi")
class LightsHoldTest {
    @get:Rule val rule = createComposeRule()

    private fun lights(on: Boolean) =
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, if (on) "luzes acesas" else "luzes apagadas"))

    @Test fun shortMissDoesNotBlink() {
        var running by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent { HomeScreen(HomeUi("14:32", "sáb", null, "—", "—", null, null, true, null, running), {}, {}, {}, {}, {}, {}, {}) }
        rule.mainClock.advanceTimeBy(100)
        lights(true).assertExists()
        running = false; Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(500)
        lights(true).assertExists()
        running = true; Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(3_000)
        lights(true).assertExists()
    }

    @Test fun engineOffTurnsLightsOff() {
        var running by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setContent { HomeScreen(HomeUi("14:32", "sáb", null, "—", "—", null, null, true, null, running), {}, {}, {}, {}, {}, {}, {}) }
        rule.mainClock.advanceTimeBy(100)
        running = false; Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(3_000)
        lights(false).assertExists()
    }
}
