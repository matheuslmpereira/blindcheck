package com.theustech.blindcheck_focus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityFocusResetReadinessTest {

    @Test
    fun `resets once the subtree is laid out and accessibility is on`() {
        assertTrue(shouldReset())
    }

    @Test
    fun `does not reset before the subtree is laid out`() {
        assertFalse(shouldReset(hasSubtreeLayout = false))
    }

    @Test
    fun `does not reset when no screen reader is listening`() {
        assertFalse(shouldReset(isAccessibilityEnabled = false))
    }

    @Test
    fun `does not reset twice for the same destination key`() {
        assertFalse(shouldReset(hasAlreadyReset = true))
    }

    @Test
    fun `does not pull focus into content that is leaving`() {
        assertFalse(shouldReset(isContentShowing = false))
    }

    @Test
    fun `respects the opt out`() {
        assertFalse(shouldReset(isEnabled = false))
    }

    @Test
    fun `retires content the moment it stops being shown`() {
        assertTrue(shouldRetire(isContentShowing = false))
    }

    @Test
    fun `does not retire the content that is being shown`() {
        assertFalse(shouldRetire(isContentShowing = true))
    }

    @Test
    fun `retires once, because the subtree is gone from the tree afterwards`() {
        assertFalse(shouldRetire(isContentShowing = false, hasAlreadyRetired = true))
    }

    @Test
    fun `does not retire when no screen reader is listening`() {
        assertFalse(shouldRetire(isContentShowing = false, isAccessibilityEnabled = false))
    }

    @Test
    fun `retiring respects the opt out`() {
        assertFalse(shouldRetire(isContentShowing = false, isEnabled = false))
    }

    private fun shouldReset(
        isEnabled: Boolean = true,
        isAccessibilityEnabled: Boolean = true,
        isContentShowing: Boolean = true,
        hasSubtreeLayout: Boolean = true,
        hasAlreadyReset: Boolean = false,
    ): Boolean =
        shouldResetAccessibilityFocus(
            isEnabled = isEnabled,
            isAccessibilityEnabled = isAccessibilityEnabled,
            isContentShowing = isContentShowing,
            hasSubtreeLayout = hasSubtreeLayout,
            hasAlreadyReset = hasAlreadyReset,
        )

    private fun shouldRetire(
        isEnabled: Boolean = true,
        isAccessibilityEnabled: Boolean = true,
        isContentShowing: Boolean = true,
        hasAlreadyRetired: Boolean = false,
    ): Boolean =
        shouldRetireLeavingContent(
            isEnabled = isEnabled,
            isAccessibilityEnabled = isAccessibilityEnabled,
            isContentShowing = isContentShowing,
            hasAlreadyRetired = hasAlreadyRetired,
        )
}
