package com.theustech.blindcheck_focus

/**
 * Gate for the single accessibility-focus reset of a destination.
 *
 * The reset must not run before the subtree has been laid out, must not run when no screen reader
 * is listening, must not run for content that is already on its way out, and must never run twice
 * for the same destination key: a second reset would pull focus away from wherever the person had
 * already navigated to.
 */
internal fun shouldResetAccessibilityFocus(
    isEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isContentShowing: Boolean,
    hasSubtreeLayout: Boolean,
    hasAlreadyReset: Boolean,
): Boolean =
    isEnabled &&
        isAccessibilityEnabled &&
        isContentShowing &&
        hasSubtreeLayout &&
        !hasAlreadyReset

/**
 * Gate for retiring content that stopped being current.
 *
 * This is the mirror image of the reset gate: it fires exactly when the content is no longer
 * showing, and only once, because the subtree is dropped from the accessibility tree in the process
 * and there is nothing left to clear on a second pass.
 *
 * Layout is not a condition here. Retiring removes accessibility rather than adding it, so content
 * that never finished laying out is content that has nothing to retire and nothing to lose.
 */
internal fun shouldRetireLeavingContent(
    isEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isContentShowing: Boolean,
    hasAlreadyRetired: Boolean,
): Boolean =
    isEnabled &&
        isAccessibilityEnabled &&
        !isContentShowing &&
        !hasAlreadyRetired
