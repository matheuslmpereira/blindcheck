package com.theustech.blindcheck_testeapp

import com.theustech.blindcheck_testing.android.AndroidAccessibilitySetup
import com.theustech.blindcheck_testing.android.AndroidAccessibilityTestDriver
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression test for TalkBack focus retained across Navigation Compose destinations.
 *
 * Run only on a dedicated emulator that has TalkBack installed:
 * ./gradlew :blindcheck-test-app:connectedDebugAndroidTest -PrunTalkBackFocusTests=true
 */
class NavGraphAccessibilityFocusTest {

    private val setup = AndroidAccessibilitySetup.create()
    private val driver = AndroidAccessibilityTestDriver.create(synchronizeWithUiIdle = false)
    private var settingsSnapshot: AndroidAccessibilitySetup.AccessibilitySettingsSnapshot? = null

    @Before
    fun enableTalkBackForOptInTest() {
        assumeTrue(
            "Pass -PrunTalkBackFocusTests=true to run TalkBack focus tests.",
            setup.isTalkBackFocusTestEnabled(),
        )
        assumeTrue("TalkBack must be installed on the dedicated test emulator.", setup.isTalkBackInstalled())

        settingsSnapshot = setup.captureAccessibilitySettings()
        setup.enableTalkBack()
        // Once the run is explicitly opted in, a TalkBack that never binds is a failure:
        // skipping here would report green without ever exercising the reset.
        assertTrue("TalkBack did not bind within the test setup timeout.", setup.waitForTalkBackService())
    }

    @After
    fun restoreAccessibilitySettings() {
        settingsSnapshot?.let(setup::restoreAccessibilitySettings)
    }

    @Test
    fun navGraphNavigation_resetsAccessibilityFocusToDestinationTitle() {
        launchNavGraphApproach(NavGraphAccessibilityApproach.ImperativeFocus).use {
            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 1"))

            activateWithAccessibilityFocus("Continuar")

            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 2"))
            driver.assertFocused(FocusExpectation(textEquals = "Tela 2"))
        }
    }
}
