package com.theustech.blindcheck_testeapp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {

    @Test
    fun `requests initial accessibility focus only when the destination is ready`() {
        assertTrue(
            shouldRequestInitialAccessibilityFocus(
                isDestinationResumed = true,
                hasRootLayout = true,
                hasRegisteredTarget = true,
                hasAlreadyRequestedFocus = false,
            ),
        )
    }

    @Test
    fun `does not request focus before destination resumes`() {
        assertFalse(
            shouldRequestInitialAccessibilityFocus(
                isDestinationResumed = false,
                hasRootLayout = true,
                hasRegisteredTarget = true,
                hasAlreadyRequestedFocus = false,
            ),
        )
    }

    @Test
    fun `does not request focus before root layout or target registration`() {
        assertFalse(
            shouldRequestInitialAccessibilityFocus(
                isDestinationResumed = true,
                hasRootLayout = false,
                hasRegisteredTarget = true,
                hasAlreadyRequestedFocus = false,
            ),
        )
        assertFalse(
            shouldRequestInitialAccessibilityFocus(
                isDestinationResumed = true,
                hasRootLayout = true,
                hasRegisteredTarget = false,
                hasAlreadyRequestedFocus = false,
            ),
        )
    }

    @Test
    fun `does not request initial focus twice for the same back stack entry`() {
        assertFalse(
            shouldRequestInitialAccessibilityFocus(
                isDestinationResumed = true,
                hasRootLayout = true,
                hasRegisteredTarget = true,
                hasAlreadyRequestedFocus = true,
            ),
        )
    }
}
