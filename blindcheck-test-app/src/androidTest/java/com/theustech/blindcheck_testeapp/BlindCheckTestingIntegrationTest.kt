package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.theustech.blindcheck_testing.android.AndroidAccessibilityTestDriver
import com.theustech.blindcheck_testing.assertions.FeedbackExpectation
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import com.theustech.blindcheck_testing.assertions.FocusSequenceExpectation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class BlindCheckTestingIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val driver = AndroidAccessibilityTestDriver.create()

    // region Login screen

    @Test
    fun loginScreen_exposesExpectedAccessibilityNodes() {
        composeRule.waitForIdle()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Acessar conta"))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "E-mail", editable = true))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Senha", editable = true))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Entrar", clickable = true))
    }

    @Test
    fun loginScreen_focusSequenceMatchesReadingOrder() {
        composeRule.waitForIdle()

        FocusSequenceExpectation(
            items = listOf(
                FocusExpectation(textContains = "Acessar conta"),
                FocusExpectation(textContains = "E-mail", editable = true),
                FocusExpectation(textContains = "Senha", editable = true),
                FocusExpectation(textContains = "Entrar", clickable = true),
            ),
        ).assertMatches(
            events = driver.currentWindowEvents(),
            targetPackage = "com.theustech.blindcheck_testeapp",
        )
    }

    @Test
    fun emptySubmit_exposesAccessibleErrorFeedback() {
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Informe o e-mail").assertIsDisplayed()
        composeRule.onNodeWithText("Informe a senha").assertIsDisplayed()

        driver.assertCurrentWindowFeedback(FeedbackExpectation(contains = "Informe o e-mail"))
        driver.assertCurrentWindowFeedback(FeedbackExpectation(contains = "Informe a senha"))
    }

    @Test
    fun partialSubmit_onlyMissingFieldShowsError() {
        composeRule.onNodeWithText("E-mail").performTextInput("dev@example.com")
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Informe a senha").assertIsDisplayed()
        driver.assertCurrentWindowFeedback(FeedbackExpectation(contains = "Informe a senha"))

        // Login screen stays (no navigation happened)
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Acessar conta"))
    }

    @Test
    fun validCredentials_navigatesToFruitList() {
        composeRule.onNodeWithText("E-mail").performTextInput("dev@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("123456")
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Frutas").assertIsDisplayed()
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    // endregion

    // region Fruit list screen

    @Test
    fun fruitListScreen_exposesVisibleFruitsAsAccessibleNodes() {
        navigateToFruitList()

        // Verify the first several fruits that are guaranteed to appear in the initial viewport
        listOf("Banana", "Laranja", "Uva", "Abacaxi").forEach { name ->
            driver.assertCurrentWindowContains(FocusExpectation(textContains = name))
        }
    }

    @Test
    fun fruitListScreen_hasClickableFruitContainers() {
        navigateToFruitList()

        // Card containers are clickable; their text lives in child Text nodes (not the card itself)
        driver.assertCurrentWindowContains(FocusExpectation(clickable = true))
    }

    // endregion

    // region Fruit detail screen

    @Test
    fun fruitDetailScreen_exposesExpectedAccessibilityNodes() {
        navigateToFruitDetail("Banana")

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Voltar", clickable = true))
        driver.assertCurrentWindowContains(FocusExpectation(contentDescriptionContains = "Imagem de Banana"))
        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta amarela, doce e facil de descascar."),
        )
    }

    @Test
    fun fruitDetailScreen_contentDescriptionMatchesSelectedFruit() {
        navigateToFruitDetail("Laranja")

        driver.assertCurrentWindowContains(FocusExpectation(contentDescriptionContains = "Imagem de Laranja"))
        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta citrica com gomos e bastante suco."),
        )
    }

    // endregion

    // region Navigation

    @Test
    fun mockFlow_canBeAssertedThroughAndroidAccessibilityAndNavigateBackWithActions() = runTest {
        composeRule.onNodeWithText("E-mail").performTextInput("dev@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("123456")
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Frutas").assertIsDisplayed()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))

        composeRule.onNodeWithText("Banana").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Voltar").assertIsDisplayed()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Voltar", clickable = true))
        driver.assertCurrentWindowContains(FocusExpectation(contentDescriptionContains = "Imagem de Banana"))
        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta amarela, doce e facil de descascar."),
        )

        driver.actions().back()
        composeRule.waitForIdle()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    @Test
    fun backButtonOnDetailScreen_returnsToFruitList() {
        navigateToFruitDetail("Manga")

        composeRule.onNodeWithText("Voltar").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Frutas").assertIsDisplayed()
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    // endregion

    // region AndroidUserAccessibilityActions unsupported operations

    @Test
    fun actions_next_throwsUnsupportedOperationException() = runTest {
        assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { driver.actions().next() }
        }
    }

    @Test
    fun actions_previous_throwsUnsupportedOperationException() = runTest {
        assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { driver.actions().previous() }
        }
    }

    @Test
    fun actions_scrollForward_throwsUnsupportedOperationException() = runTest {
        assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { driver.actions().scrollForward() }
        }
    }

    @Test
    fun actions_scrollBackward_throwsUnsupportedOperationException() = runTest {
        assertThrows(UnsupportedOperationException::class.java) {
            kotlinx.coroutines.runBlocking { driver.actions().scrollBackward() }
        }
    }

    // endregion

    // region Helpers

    private fun navigateToFruitList() {
        composeRule.onNodeWithText("E-mail").performTextInput("dev@example.com")
        composeRule.onNodeWithText("Senha").performTextInput("123456")
        composeRule.onNodeWithText("Entrar").performClick()
        composeRule.waitForIdle()
    }

    private fun navigateToFruitDetail(fruitName: String) {
        navigateToFruitList()
        composeRule.onNodeWithText(fruitName).performClick()
        composeRule.waitForIdle()
    }

    // endregion
}
