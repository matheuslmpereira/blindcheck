package com.theustech.blindcheck_focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityFocusResetReadinessTest {

    @Test
    fun `resets once the subtree is laid out and accessibility is on`() {
        assertTrue(
            shouldResetAccessibilityFocus(
                isEnabled = true,
                isAccessibilityEnabled = true,
                hasSubtreeLayout = true,
                hasAlreadyReset = false,
            ),
        )
    }

    @Test
    fun `does not reset before the subtree is laid out`() {
        assertFalse(
            shouldResetAccessibilityFocus(
                isEnabled = true,
                isAccessibilityEnabled = true,
                hasSubtreeLayout = false,
                hasAlreadyReset = false,
            ),
        )
    }

    @Test
    fun `does not reset when no screen reader is listening`() {
        assertFalse(
            shouldResetAccessibilityFocus(
                isEnabled = true,
                isAccessibilityEnabled = false,
                hasSubtreeLayout = true,
                hasAlreadyReset = false,
            ),
        )
    }

    @Test
    fun `does not reset twice for the same destination key`() {
        assertFalse(
            shouldResetAccessibilityFocus(
                isEnabled = true,
                isAccessibilityEnabled = true,
                hasSubtreeLayout = true,
                hasAlreadyReset = true,
            ),
        )
    }

    @Test
    fun `respects the opt out`() {
        assertFalse(
            shouldResetAccessibilityFocus(
                isEnabled = false,
                isAccessibilityEnabled = true,
                hasSubtreeLayout = true,
                hasAlreadyReset = false,
            ),
        )
    }
}
