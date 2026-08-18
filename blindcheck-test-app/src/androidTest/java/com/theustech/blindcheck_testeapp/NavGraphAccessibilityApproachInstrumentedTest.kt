package com.theustech.blindcheck_testeapp

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.theustech.blindcheck_testing.android.AndroidAccessibilityTestDriver
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavGraphAccessibilityApproachInstrumentedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val accessibilityDriver = AndroidAccessibilityTestDriver.create()

    @Test
    fun everyApproachNavigatesThroughThreeDestinations() {
        NavGraphAccessibilityApproach.experiments.forEach { approach ->
            launch(approach).use {
                composeRule.onAllNodesWithContentDescription("Ir para home").assertCountEquals(0)
                assertScreenTitle(approach, 1)
                composeRule.onNodeWithText(approach.continueLabel(1)).performClick()
                assertScreenTitle(approach, 2)
                composeRule.onNodeWithText(approach.continueLabel(2)).performClick()
                assertScreenTitle(approach, 3)
                composeRule.onNodeWithText(approach.continueLabel(3)).assertIsDisplayed()
            }
        }
    }

    @Test
    fun uniqueNodeIdApproachExportsPageSpecificIdsToAndroidAccessibilityTree() {
        launch(NavGraphAccessibilityApproach.UniqueNodeIds).use {
            composeRule.onNodeWithTag("navgraph_continue_page_1").assertIsDisplayed()
            assertAndroidResourceId("navgraph_continue_page_1")

            composeRule.onNodeWithText("Continuar").performClick()
            composeRule.onNodeWithText("Tela 2").assertIsDisplayed()
            composeRule.onNodeWithTag("navgraph_continue_page_2").assertIsDisplayed()
            assertAndroidResourceId("navgraph_continue_page_2")
        }
    }

    @Test
    fun paneTitleApproachUpdatesDestinationPaneSemantics() {
        launch(NavGraphAccessibilityApproach.PaneTitle).use {
            composeRule.onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Tela 1"),
            ).assertIsDisplayed()

            composeRule.onNodeWithText("Continuar").performClick()

            composeRule.onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Tela 2"),
            ).assertIsDisplayed()
        }
    }

    @Test
    fun imperativeFocusApproachRendersEachNewDestinationWithoutHome() {
        launch(NavGraphAccessibilityApproach.ImperativeFocus).use {
            onView(withText("Tela 1")).check(matches(isDisplayed()))
            composeRule.onAllNodesWithContentDescription("Ir para home").assertCountEquals(0)

            composeRule.onNodeWithText("Continuar").performClick()

            onView(withText("Tela 2")).check(matches(isDisplayed()))
            composeRule.onAllNodesWithContentDescription("Ir para home").assertCountEquals(0)
        }
    }

    private fun launch(
        approach: NavGraphAccessibilityApproach,
    ): ActivityScenario<MainActivity> {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVGRAPH_ACCESSIBILITY_APPROACH, approach.argumentValue)
        }
        return ActivityScenario.launch(intent)
    }

    private fun assertAndroidResourceId(expectedId: String) {
        accessibilityDriver.assertCurrentWindowContains(
            FocusExpectation(viewIdResourceNameEquals = expectedId),
        )
    }

    private fun assertScreenTitle(approach: NavGraphAccessibilityApproach, page: Int) {
        if (approach.requestsImperativeAccessibilityFocus) {
            onView(withText("Tela $page")).check(matches(isDisplayed()))
        } else {
            composeRule.onNodeWithText("Tela $page").assertIsDisplayed()
        }
    }
}
