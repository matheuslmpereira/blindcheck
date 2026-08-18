package com.theustech.blindcheck_testeapp

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.theustech.blindcheck_testing.android.AndroidAccessibilitySetup
import com.theustech.blindcheck_testing.android.AndroidAccessibilityTestDriver
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Duplicate of [NavGraphAccessibilityFocusTest] for the content-agnostic library approach.
 *
 * The destinations keep the ambiguous `Continuar` label on every page and register nothing: the
 * reset comes only from `Modifier.resetAccessibilityFocusOnEnter`, so this test fails if the
 * library stops resolving the first accessible item of the new destination.
 *
 * Run only on a dedicated emulator that has TalkBack installed:
 * ./gradlew :blindcheck-test-app:connectedDebugAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.runTalkBackFocusTests=true
 */
class NavGraphAgnosticFocusResetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val setup = AndroidAccessibilitySetup.create()
    private val driver = AndroidAccessibilityTestDriver.create(synchronizeWithUiIdle = false)
    private var settingsSnapshot: AndroidAccessibilitySetup.AccessibilitySettingsSnapshot? = null

    @Before
    fun enableTalkBackForOptInTest() {
        assumeTrue(
            "Pass -e ${AndroidAccessibilitySetup.TALKBACK_FOCUS_TEST_ARGUMENT} true to run TalkBack focus tests.",
            setup.isTalkBackFocusTestEnabled(),
        )
        assumeTrue("TalkBack must be installed on the dedicated test emulator.", setup.isTalkBackInstalled())

        settingsSnapshot = setup.captureAccessibilitySettings()
        setup.enableTalkBack()
        assumeTrue("TalkBack did not bind within the test setup timeout.", setup.waitForTalkBackService())
    }

    @After
    fun restoreAccessibilitySettings() {
        settingsSnapshot?.let(setup::restoreAccessibilitySettings)
    }

    @Test
    fun libraryResetMovesFocusToTheFirstAccessibleItemOfEachDestination() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.intent.putExtra(
                EXTRA_NAVGRAPH_ACCESSIBILITY_APPROACH,
                NavGraphAccessibilityApproach.AgnosticFocusReset.argumentValue,
            )
            activity.recreate()
        }
        driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 1"))

        composeRule.onNodeWithText("Continuar").performClick()

        driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 2"))
        driver.assertFocused(FocusExpectation(textEquals = "Tela 2"))

        composeRule.onNodeWithText("Continuar").performClick()

        driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 3"))
        driver.assertFocused(FocusExpectation(textEquals = "Tela 3"))
    }
}
