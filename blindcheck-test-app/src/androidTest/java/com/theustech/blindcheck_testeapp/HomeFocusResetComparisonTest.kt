package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two focus-reset entry points offered side by side on the home screen.
 *
 * Both flows keep the same ambiguous `Continuar` label on every destination, so the only variable
 * between them is where the reset target comes from. These tests pin the entry points and the
 * structural difference; the speech each one produces is measured by the controlled TTS matrix.
 */
@RunWith(AndroidJUnit4::class)
class HomeFocusResetComparisonTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun imperativeComparison_registersItsTargetAsANativeView() {
        composeRule.onNodeWithText(IMPERATIVE_ENTRY).performClick()

        // The initial method needs a native View to act on, so the title is not a Compose node.
        onView(withText("Tela 1")).check(matches(isDisplayed()))
        composeRule.onAllNodesWithText("Tela 1").assertCountEquals(0)

        composeRule.onNodeWithText("Continuar").performClick()

        onView(withText("Tela 2")).check(matches(isDisplayed()))
    }

    @Test
    fun libraryComparison_keepsEveryDestinationInPlainCompose() {
        composeRule.onNodeWithText(LIBRARY_ENTRY).performClick()

        composeRule.onNodeWithText("Tela 1").assertIsDisplayed()

        composeRule.onNodeWithText("Continuar").performClick()

        composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed()
    }

    @Test
    fun bothComparisonsUseTheSameAmbiguousLabel() {
        composeRule.onNodeWithText(IMPERATIVE_ENTRY).assertIsDisplayed()
        composeRule.onNodeWithText(LIBRARY_ENTRY).assertIsDisplayed()

        listOf(
            NavGraphAccessibilityApproach.ImperativeFocus,
            NavGraphAccessibilityApproach.AgnosticFocusReset,
        ).forEach { approach ->
            (1..3).forEach { page ->
                assert(approach.continueLabel(page) == "Continuar") {
                    "$approach must keep the ambiguous label to reproduce the problem"
                }
            }
        }
    }
}

private const val IMPERATIVE_ENTRY = "Comparação: foco imperativo (método inicial)"
private const val LIBRARY_ENTRY = "Comparação: reset agnóstico pela lib"
