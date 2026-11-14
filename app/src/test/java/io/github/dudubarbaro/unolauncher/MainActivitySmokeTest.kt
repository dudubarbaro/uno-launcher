package io.github.dudubarbaro.unolauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Abre o app de verdade (UnoApp + MainActivity) e navega pelas telas principais sem travar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1024dp-h600dp-land-mdpi")
class MainActivitySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun opensAndNavigates() {
        rule.onNodeWithText("Waze").assertExists()
        rule.onRoot().captureRoboImage("build/shots/app-inicio.png")
        rule.onNodeWithText("OBD2").performClick()
        rule.onNodeWithText("Códigos de falha").performClick()
        rule.onNodeWithText("Ler de novo").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("estimativa · toque para ver as viagens").performClick()
        rule.onNodeWithText("Nova viagem").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed(); it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("⚙").performClick()
        rule.onNodeWithText("Foto do Uno").assertExists()
    }

    /** Voltar de uma opção do fim da lista não pode jogar Configurações para o topo. */
    @Test fun settingsKeepsScrollAfterSubScreen() {
        rule.onNodeWithText("⚙").performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Sobre este aparelho"))
        rule.onNodeWithText("Sobre este aparelho").performClick()
        rule.onNodeWithText("Versão do app").assertExists()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        rule.onNodeWithText("Sobre este aparelho").assertIsDisplayed()
    }
}
