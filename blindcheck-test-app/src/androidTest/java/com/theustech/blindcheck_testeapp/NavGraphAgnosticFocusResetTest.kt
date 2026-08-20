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
 * Duplicate of [NavGraphAccessibilityFocusTest] for the content-agnostic library approach.
 *
 * The destinations keep the ambiguous `Continuar` label on every page and register nothing: the
 * reset comes only from `Modifier.resetAccessibilityFocusOnEnter`, so this test fails if the
 * library stops resolving the first accessible item of the new destination.
 *
 * Activation goes through the accessibility node, because an injected tap is swallowed by
 * TalkBack's touch exploration. One transition is asserted here; the full three-destination sweep
 * driven by real TalkBack gestures is covered by the controlled TTS matrix.
 *
 * Run only on a dedicated emulator that has TalkBack installed:
 * ./gradlew :blindcheck-test-app:connectedDebugAndroidTest -PrunTalkBackFocusTests=true
 */
class NavGraphAgnosticFocusResetTest {

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
    fun libraryResetMovesFocusToTheFirstAccessibleItemOfEachDestination() {
        launchNavGraphApproach(NavGraphAccessibilityApproach.AgnosticFocusReset).use {
            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 1"))
            driver.assertFocused(FocusExpectation(textEquals = "Tela 1"))

            activateWithAccessibilityFocus("Continuar")

            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 2"))
            driver.assertFocused(FocusExpectation(textEquals = "Tela 2"))
        }
    }
}
