package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The bottom sheet scenario, reachable from the home screen.
 *
 * A sheet is a second container inside the same window, so opening it is not a destination change.
 * These tests pin the flow and the stable labels; what a screen reader announces and where focus
 * lands when the sheet opens and closes is measured by the controlled TTS capture.
 */
@RunWith(AndroidJUnit4::class)
class BottomSheetScenarioTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun scenarioIsReachableFromTheHomeScreen() {
        composeRule.onNodeWithText(ENTRY).assertIsDisplayed()

        composeRule.onNodeWithText(ENTRY).performClick()

        composeRule.onNodeWithText("Tela com bottom sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir detalhes").assertIsDisplayed()
        composeRule.onAllNodesWithText("Detalhes do pedido").assertCountEquals(0)
    }

    @Test
    fun openingTheSheetShowsItsTitleTextAndCloseAction() {
        openScenario()

        composeRule.onNodeWithText("Abrir detalhes").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Detalhes do pedido").assertIsDisplayed()
        composeRule.onNodeWithText("O pedido foi confirmado e sera entregue em ate tres dias uteis.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Fechar").assertIsDisplayed()
    }

    @Test
    fun closingTheSheetReturnsToTheScreenBehindIt() {
        openScenario()
        composeRule.onNodeWithText("Abrir detalhes").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Fechar").performClick()
        composeRule.waitUntil(WAIT_FOR_SHEET_MS) {
            composeRule.onAllNodesWithText("Detalhes do pedido").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithText("Tela com bottom sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Abrir detalhes").assertIsDisplayed()
    }

    @Test
    fun homeActionReturnsToTheScenarioSelection() {
        openScenario()

        composeRule.onNodeWithContentDescription("Ir para home").performClick()

        composeRule.onNodeWithText(ENTRY).assertIsDisplayed()
    }

    private fun openScenario() {
        composeRule.onNodeWithText(ENTRY).performClick()
        composeRule.waitForIdle()
    }
}

private const val ENTRY = "Iniciar cenário de bottom sheet"
private const val WAIT_FOR_SHEET_MS = 5_000L
