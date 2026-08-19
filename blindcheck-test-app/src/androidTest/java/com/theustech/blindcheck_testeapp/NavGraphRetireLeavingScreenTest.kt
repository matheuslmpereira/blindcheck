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
 * Guard for the third approach: the destination that leaves clears its accessibility focus and
 * drops its whole subtree from the accessibility tree, and nobody requests focus anywhere.
 *
 * What this test can prove is that retiring hits the right subtree at the right moment. The
 * dangerous failure mode of this strategy is retiring the arriving destination — `clearAndSetSemantics`
 * on the wrong side leaves a screen that renders normally and is invisible to a screen reader, which
 * no visual check would catch. So the assertion is that the new destination is fully exposed after
 * the transition.
 *
 * Where the reader actually lands afterwards is the open question this scenario exists to answer,
 * and a test cannot assert an unknown: that outcome is measured by the controlled TTS matrix
 * (`NAVGRAPH_TTS_RUNS=3 make navgraph-tts-matrix`), not here.
 *
 * Run only on a dedicated emulator that has TalkBack installed:
 * ./gradlew :blindcheck-test-app:connectedDebugAndroidTest -PrunTalkBackFocusTests=true
 */
class NavGraphRetireLeavingScreenTest {

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
        assertTrue("TalkBack did not bind within the test setup timeout.", setup.waitForTalkBackService())
    }

    @After
    fun restoreAccessibilitySettings() {
        settingsSnapshot?.let(setup::restoreAccessibilitySettings)
    }

    @Test
    fun retiringTheLeavingScreenKeepsTheArrivingOneReadable() {
        launchNavGraphApproach(NavGraphAccessibilityApproach.RetireLeavingScreen).use {
            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 1"))
            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Continuar"))

            activateWithAccessibilityFocus("Continuar")

            // Both items of the arriving destination, so a retirement applied to the wrong side of
            // the transition fails here instead of shipping a screen a reader cannot see.
            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Tela 2"))
            driver.assertCurrentWindowContains(FocusExpectation(textEquals = "Continuar"))
        }
    }
}
