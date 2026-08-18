package com.theustech.blindcheck_focus

/**
 * Gate for the single accessibility-focus reset of a destination.
 *
 * The reset must not run before the subtree has been laid out, must not run when no screen reader
 * is listening, and must never run twice for the same destination key: a second reset would pull
 * focus away from wherever the person had already navigated to.
 */
internal fun shouldResetAccessibilityFocus(
    isEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    hasSubtreeLayout: Boolean,
    hasAlreadyReset: Boolean,
): Boolean =
    isEnabled &&
        isAccessibilityEnabled &&
        hasSubtreeLayout &&
        !hasAlreadyReset
