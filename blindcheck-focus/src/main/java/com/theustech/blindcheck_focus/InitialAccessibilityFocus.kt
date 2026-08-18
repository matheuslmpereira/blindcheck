package com.theustech.blindcheck_focus

/**
 * Content-agnostic description of one accessibility node inside a focus-reset root.
 *
 * The library never inspects titles, buttons, or any app-specific concept: it only reads the
 * generic accessibility traits that a screen reader itself uses to decide what can hold focus.
 */
data class AccessibilityFocusCandidate(
    val nodeId: Int,
    val top: Float,
    val left: Float,
    val width: Float,
    val height: Float,
    val traversalIndex: Float = 0f,
    val isHiddenFromAccessibility: Boolean = false,
    val describesItself: Boolean = false,
    val isActionable: Boolean = false,
)

/**
 * A node can hold screen-reader focus when it is not hidden, occupies space, and either describes
 * itself (text, content description, state description) or exposes an action.
 */
fun AccessibilityFocusCandidate.canHoldScreenReaderFocus(): Boolean =
    !isHiddenFromAccessibility &&
        width > 0f &&
        height > 0f &&
        (describesItself || isActionable)

/**
 * Returns the node a screen reader would visit first inside the reset root, or `null` when the
 * subtree still has nothing focusable.
 *
 * Ordering mirrors the reading order: `traversalIndex` first (lower comes first, which is how
 * `traversalIndex = -1f` promotes a node), then top-to-bottom, then left-to-right. The node id is
 * the final tiebreaker so the result is deterministic for perfectly overlapping nodes.
 */
fun selectInitialAccessibilityFocus(candidates: List<AccessibilityFocusCandidate>): Int? =
    candidates
        .filter { it.canHoldScreenReaderFocus() }
        .minWithOrNull(
            compareBy(
                { it.traversalIndex },
                { it.top },
                { it.left },
                { it.nodeId },
            ),
        )
        ?.nodeId
