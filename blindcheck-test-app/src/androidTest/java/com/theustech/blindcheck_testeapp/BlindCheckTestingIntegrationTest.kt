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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class BlindCheckTestingIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val driver = AndroidAccessibilityTestDriver.create()

    @Test
    fun loginScreen_exposesExpectedAccessibilityNodes() {
        composeRule.waitForIdle()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Acessar conta"))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "E-mail", editable = true))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Senha", editable = true))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Entrar", clickable = true))

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
    fun mockFlow_canBeAssertedThroughAndroidAccessibilityAndNavigateBackWithActions() {
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

        runSuspend { driver.actions().back() }
        composeRule.waitForIdle()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    private fun runSuspend(block: suspend () -> Unit) {
        var failure: Throwable? = null
        block.startCoroutine(
            object : Continuation<Unit> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<Unit>) {
                    failure = result.exceptionOrNull()
                }
            },
        )
        failure?.let { throw it }
    }
}
