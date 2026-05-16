package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.theustech.blindcheck_testing.android.AndroidAccessibilitySetup
import com.theustech.blindcheck_testing.android.AndroidAccessibilityTestDriver
import com.theustech.blindcheck_testing.assertions.FeedbackExpectation
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import com.theustech.blindcheck_testing.assertions.FocusSequenceExpectation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Blind-user flow tests for the BlindCheck mock app.
 *
 * All interaction in this file goes through Android accessibility APIs — the same channel
 * used by TalkBack. Compose test APIs (performClick, performTextInput) are intentionally
 * avoided so the tests validate the experience a blind user actually has.
 *
 * Execution model:
 *   focusFirst(expectation)      → moves accessibility focus to a node
 *   actions().inputText(value)   → sets text on the focused editable node
 *   actions().activate()         → performs ACTION_CLICK on the focused node
 *   actions().back()             → triggers Android back navigation
 *
 * Assertions observe the Android accessibility tree, not the Compose semantics tree
 * or any visual state. This corresponds to layer 3 in the BlindCheck accessibility model:
 *   Compose Semantics Tree → Android Accessibility Tree ← (asserted here)
 */
class BlindUserFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val setup = AndroidAccessibilitySetup.create()
    private val driver = AndroidAccessibilityTestDriver.create()

    @Before
    fun enableAccessibility() {
        setup.ensureAccessibilityEnabled()
        composeRule.waitForIdle()
    }

    // ── Login screen ──────────────────────────────────────────────────────────

    @Test
    fun loginScreen_focusSequence_matchesExpectedReadingOrder() {
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
    fun loginScreen_title_isFocusableByScreenReader() {
        val reached = driver.focusFirst(FocusExpectation(textContains = "Acessar conta"))
        assertTrue("Heading 'Acessar conta' must be reachable via accessibility focus", reached)
    }

    @Test
    fun loginScreen_emailField_isFocusableEditableAndLabeled() {
        val reached = driver.focusFirst(FocusExpectation(textContains = "E-mail", editable = true))
        assertTrue("E-mail field must be focusable via accessibility", reached)
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "E-mail", editable = true))
    }

    @Test
    fun loginScreen_passwordField_isFocusableEditableAndLabeled() {
        val reached = driver.focusFirst(FocusExpectation(textContains = "Senha", editable = true))
        assertTrue("Senha field must be focusable via accessibility", reached)
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Senha", editable = true))
    }

    @Test
    fun loginScreen_submitButton_isFocusableClickableAndLabeled() {
        val reached = driver.focusFirst(FocusExpectation(textContains = "Entrar", clickable = true))
        assertTrue("Entrar button must be focusable via accessibility", reached)
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Entrar", clickable = true))
    }

    // ── Login error flow via accessibility ────────────────────────────────────

    @Test
    fun loginErrorFlow_submitWithoutFields_errorFeedbackIsAccessible() {
        driver.focusFirst(FocusExpectation(textContains = "Entrar", clickable = true))
        runBlocking { driver.actions().activate() }
        composeRule.waitForIdle()

        driver.assertCurrentWindowFeedback(FeedbackExpectation(contains = "Informe o e-mail"))
        driver.assertCurrentWindowFeedback(FeedbackExpectation(contains = "Informe a senha"))
    }

    @Test
    fun loginErrorFlow_errorMessages_areInAccessibilityTree() {
        driver.focusFirst(FocusExpectation(textContains = "Entrar", clickable = true))
        runBlocking { driver.actions().activate() }
        composeRule.waitForIdle()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Informe o e-mail"))
        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Informe a senha"))
    }

    // ── Login happy path via accessibility ────────────────────────────────────

    @Test
    fun loginHappyPath_fullAccessibilityNavigation_reachesFruitList() {
        loginViaAccessibility()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    @Test
    fun loginHappyPath_fruitListTitle_isFocusableAfterLogin() {
        loginViaAccessibility()

        val reached = driver.focusFirst(FocusExpectation(textContains = "Frutas"))
        assertTrue("'Frutas' heading must be focusable via accessibility after login", reached)
    }

    // ── Fruit list screen ─────────────────────────────────────────────────────

    @Test
    fun fruitListScreen_visibleFruits_areInAccessibilityTree() {
        loginViaAccessibility()

        listOf("Banana", "Laranja", "Uva", "Abacaxi").forEach { name ->
            driver.assertCurrentWindowContains(FocusExpectation(textContains = name))
        }
    }

    @Test
    fun fruitListScreen_fruitDescriptions_areInAccessibilityTree() {
        loginViaAccessibility()

        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta amarela, doce e facil de descascar."),
        )
        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta citrica com gomos e bastante suco."),
        )
    }

    @Test
    fun fruitListScreen_bananaItem_isFocusableViaAccessibility() {
        loginViaAccessibility()

        val reached = driver.focusFirst(FocusExpectation(textContains = "Banana"))
        assertTrue("Banana item must be focusable via accessibility", reached)
    }

    // ── Fruit detail screen ───────────────────────────────────────────────────

    @Test
    fun fruitDetailScreen_imageHasContentDescription() {
        loginViaAccessibility()
        navigateToFruitDetailViaAccessibility("Banana")

        driver.assertCurrentWindowContains(
            FocusExpectation(contentDescriptionContains = "Imagem de Banana"),
        )
    }

    @Test
    fun fruitDetailScreen_fruitTitle_isAccessible() {
        loginViaAccessibility()
        navigateToFruitDetailViaAccessibility("Banana")

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Banana"))
    }

    @Test
    fun fruitDetailScreen_fruitDescription_isAccessible() {
        loginViaAccessibility()
        navigateToFruitDetailViaAccessibility("Banana")

        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta amarela, doce e facil de descascar."),
        )
    }

    @Test
    fun fruitDetailScreen_backButton_isFocusableClickableAndLabeled() {
        loginViaAccessibility()
        navigateToFruitDetailViaAccessibility("Banana")

        val reached = driver.focusFirst(FocusExpectation(textContains = "Voltar", clickable = true))
        assertTrue("'Voltar' button must be focusable and clickable via accessibility", reached)
    }

    @Test
    fun fruitDetailScreen_activatingBack_returnsToPreviousScreen() {
        loginViaAccessibility()
        navigateToFruitDetailViaAccessibility("Banana")

        driver.focusFirst(FocusExpectation(textContains = "Voltar", clickable = true))
        runBlocking { driver.actions().activate() }
        composeRule.waitForIdle()

        driver.assertCurrentWindowContains(FocusExpectation(textContains = "Frutas"))
    }

    @Test
    fun fruitDetailScreen_differentFruits_haveCorrectContentDescriptions() {
        loginViaAccessibility()
        navigateToFruitDetailViaAccessibility("Laranja")

        driver.assertCurrentWindowContains(
            FocusExpectation(contentDescriptionContains = "Imagem de Laranja"),
        )
        driver.assertCurrentWindowContains(
            FocusExpectation(textContains = "Fruta citrica com gomos e bastante suco."),
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun loginViaAccessibility() {
        driver.focusFirst(FocusExpectation(textContains = "E-mail", editable = true))
        runBlocking { driver.actions().inputText("dev@example.com") }

        driver.focusFirst(FocusExpectation(textContains = "Senha", editable = true))
        runBlocking { driver.actions().inputText("123456") }

        driver.focusFirst(FocusExpectation(textContains = "Entrar", clickable = true))
        runBlocking { driver.actions().activate() }
        composeRule.waitForIdle()
    }

    private fun navigateToFruitDetailViaAccessibility(fruitName: String) {
        driver.focusFirst(FocusExpectation(textContains = fruitName))
        runBlocking { driver.actions().activate() }
        composeRule.waitForIdle()
    }
}
