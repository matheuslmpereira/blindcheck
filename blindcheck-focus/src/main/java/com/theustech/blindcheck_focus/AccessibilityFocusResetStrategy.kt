package com.theustech.blindcheck_focus

/**
 * How a destination breaks the screen reader's "keep the equivalent node" behaviour when the
 * content changes.
 *
 * Both strategies answer the same question — after a destination change, does the person hear the
 * new screen or the same label again? — and differ only in who acts. Keeping them as one parameter
 * of one modifier is deliberate: they are alternatives to measure against each other, not two
 * public concepts a screen has to learn.
 */
enum class AccessibilityFocusResetStrategy {

    /**
     * The entering content takes focus: the first accessible item of the subtree receives
     * `ACTION_ACCESSIBILITY_FOCUS`.
     *
     * This is the strategy validated by the controlled TTS capture, and the default.
     */
    MoveFocusToFirstItem,

    /**
     * The leaving content steps aside: its accessibility focus is cleared and its whole subtree is
     * made ineligible for the screen reader, then the platform is left to place focus in whatever
     * accessible content remains.
     *
     * Nothing requests focus here. The premise is that the reader keeps focus on an equivalent node
     * only while such a node still exists, so removing the leaving destination from the
     * accessibility tree should leave the entering one as the only candidate.
     *
     * This makes the destination cooperate on its way out, so the host has to say when the content
     * stops being current — see the `isShowing` parameter of
     * [resetAccessibilityFocusOnEnter]. In Navigation Compose that is the back stack entry falling
     * below `RESUMED`, which happens while both destinations are still composed.
     */
    RetireLeavingContent,
}
