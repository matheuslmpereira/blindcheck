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
 * Regression test for TalkBack focus retained across Navigation Compose destinations.
 *
 * Run only on a dedicated emulator that has TalkBack installed:
 * ./gradlew :blindcheck-test-app:connectedDebugAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.runTalkBackFocusTests=true
 */
class NavGraphAccessibilityFocusTest {

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
    fun navGraphNavigation_resetsAccessibilityFocusToDestinationTitle() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.intent.putExtra(
                EXTRA_NAVGRAPH_ACCESSIBILITY_APPROACH,
                NavGraphAccessibilityApproach.ImperativeFocus.argumentValue,
            )
            activity.recreate()
        }
        driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 1"))

        composeRule.onNodeWithText("Continuar").performClick()

        driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 2"))
        driver.assertFocused(
            FocusExpectation(
                textEquals = "Tela 2",
            ),
        )
    }
}
