package com.theustech.blindcheck_testing.android

import android.app.Instrumentation
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.theustech.blindcheck_testing.assertions.FeedbackExpectation
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import com.theustech.blindcheck_testing.model.A11yEventRecord
import com.theustech.blindcheck_testing.model.A11yNodeSnapshot

class AndroidAccessibilityTestDriver(
    private val instrumentation: Instrumentation,
    private val nodeMapper: AndroidAccessibilityNodeMapper = AndroidAccessibilityNodeMapper(),
    private val synchronizeWithUiIdle: Boolean = true,
) {
    fun currentWindowSnapshot(): A11yNodeSnapshot {
        val root = instrumentation.uiAutomation.rootInActiveWindow
            ?: throw AssertionError("Expected an active accessibility window, but rootInActiveWindow was null.")

        return root.useNode {
            nodeMapper.map(it)
                ?: throw AssertionError("Expected an accessibility snapshot, but the active window could not be mapped.")
        }
    }

    fun currentWindowEvents(eventType: String = SYNTHETIC_SNAPSHOT_EVENT_TYPE): List<A11yEventRecord> {
        return currentWindowSnapshots().flatMap { it.flattenPreOrder() }.mapIndexed { index, node ->
            A11yEventRecord(
                id = "window-node-$index",
                timestamp = System.currentTimeMillis(),
                packageName = node.packageName,
                eventType = eventType,
                className = node.className,
                text = listOfNotNull(node.text),
                contentDescription = node.contentDescription,
                sourceNode = node,
            )
        }
    }

    fun assertCurrentWindowContains(expectation: FocusExpectation) {
        val matched = waitUntil {
            currentWindowSnapshots()
                .flatMap { it.flattenPreOrder() }
                .any { node -> expectation.matchesNodeOrActionableParent(node) }
        }
        if (!matched) {
            val observed = currentWindowSnapshots()
                .flatMap { it.flattenPreOrder() }
                .mapNotNull { node -> node.text ?: node.contentDescription }
                .distinct()
                .take(12)
                .joinToString()
            throw AssertionError(
                "Expected current accessibility window to contain [${expectation.describe()}], " +
                    "but no matching node was found. Observed labels: [$observed].",
            )
        }
    }

    fun assertCurrentWindowFeedback(expectation: FeedbackExpectation) {
        val matched = waitUntil {
            currentWindowEvents().any(expectation::matches)
        }
        if (!matched) {
            throw AssertionError(
                "Expected current accessibility window to expose feedback containing " +
                    "\"${expectation.contains}\", but no matching node text or contentDescription was found.",
            )
        }
    }

    fun assertFocused(expectation: FocusExpectation) {
        val matched = waitUntil {
            currentWindowSnapshots()
                .flatMap { it.flattenPreOrder() }
                .filter { it.focused }
                .any { node -> expectation.matchesNodeOrActionableParent(node) }
        }
        if (!matched) {
            val focused = currentWindowSnapshots()
                .flatMap { it.flattenPreOrder() }
                .filter { it.focused }
                .mapNotNull { node -> node.text ?: node.contentDescription }
                .distinct()
                .joinToString()
            throw AssertionError(
                "Expected the focused accessibility node to match [${expectation.describe()}], " +
                    "but no focused node matched. Observed focused labels: [$focused].",
            )
        }
    }

    fun focusFirst(expectation: FocusExpectation): Boolean {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        return root.useNode { node ->
            node.findFirstMatching(expectation)?.useNode { match ->
                match.performAction(AccessibilityNodeInfo.ACTION_FOCUS) or
                    match.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            } ?: false
        }
    }

    /**
     * Gives Android accessibility focus to the first matching node.
     *
     * This deliberately does not request input or keyboard focus. It is intended for
     * instrumented tests that need to establish the same focus type observed by screen readers.
     */
    fun focusFirstForAccessibility(expectation: FocusExpectation): Boolean {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return false
        return root.useNode { node ->
            node.findFirstMatching(expectation)?.useNode { match ->
                match.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            } ?: false
        }
    }

    fun actions(): AndroidUserAccessibilityActions {
        return AndroidUserAccessibilityActions(instrumentation)
    }

    private fun AccessibilityNodeInfo.findFirstMatching(
        expectation: FocusExpectation,
    ): AccessibilityNodeInfo? {
        val snapshot = nodeMapper.map(this)
        if (snapshot != null && expectation.matches(snapshot)) {
            return AccessibilityNodeInfo.obtain(this)
        }

        // Material Compose controls commonly expose clickability on a parent node and their
        // visible label on a child node. Treat that pair as a single actionable control when
        // the expectation requires an interaction state such as clickable or editable.
        if (snapshot != null && expectation.matchesNodeOrActionableParent(snapshot)
        ) {
            return AccessibilityNodeInfo.obtain(this)
        }

        for (index in 0 until childCount) {
            val child = runCatching { getChild(index) }.getOrNull() ?: continue
            val match = child.useNode { it.findFirstMatching(expectation) }
            if (match != null) return match
        }
        return null
    }

    private fun FocusExpectation.requiresInteractionState(): Boolean {
        return clickable != null || editable != null || enabled != null ||
            selected != null || checked != null
    }

    private fun FocusExpectation.matchesNodeOrActionableParent(node: A11yNodeSnapshot): Boolean {
        return matches(node) ||
            (requiresInteractionState() && matchesWithDescendantLabel(node))
    }

    private fun FocusExpectation.matchesWithDescendantLabel(node: A11yNodeSnapshot): Boolean {
        val descendants = buildList {
            fun visit(snapshot: A11yNodeSnapshot) {
                snapshot.children.forEach { child ->
                    add(child)
                    visit(child)
                }
            }
            visit(node)
        }
        val textCandidates = listOf<String?>(null) + descendants.map { it.text }
        val descriptionCandidates = listOf<String?>(null) + descendants.map { it.contentDescription }

        return textCandidates.any { text ->
            descriptionCandidates.any { contentDescription ->
                matches(node.copy(text = text, contentDescription = contentDescription))
            }
        }
    }

    private fun currentWindowSnapshots(): List<A11yNodeSnapshot> {
        if (synchronizeWithUiIdle) {
            instrumentation.waitForIdleSync()
        }
        val uiAutomation = instrumentation.uiAutomation
        val roots = buildList {
            // On a device with TalkBack enabled, UiAutomation.windows can temporarily omit
            // the active application window while the service is reacting to a window change.
            // rootInActiveWindow remains the authoritative source for that window.
            uiAutomation.rootInActiveWindow?.let(::add)
            uiAutomation.windows
                .sortedByDescending { it.isActive }
                .mapNotNull { window -> runCatching { window.root }.getOrNull() }
                .forEach(::add)
        }

        return roots
            .mapNotNull { root -> root.useNode(nodeMapper::map) }
    }

    private fun waitUntil(
        timeoutMs: Long = DEFAULT_ASSERTION_TIMEOUT_MS,
        intervalMs: Long = DEFAULT_ASSERTION_POLL_INTERVAL_MS,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            if (condition()) return true
            SystemClock.sleep(intervalMs)
        } while (SystemClock.uptimeMillis() < deadline)
        return condition()
    }

    private fun A11yNodeSnapshot.flattenPreOrder(): List<A11yNodeSnapshot> {
        return buildList {
            fun visit(node: A11yNodeSnapshot) {
                add(node)
                node.children.forEach(::visit)
            }
            visit(this@flattenPreOrder)
        }
    }


    companion object {
        const val SYNTHETIC_SNAPSHOT_EVENT_TYPE = "TYPE_SYNTHETIC_WINDOW_SNAPSHOT"
        private const val DEFAULT_ASSERTION_TIMEOUT_MS = 2_000L
        private const val DEFAULT_ASSERTION_POLL_INTERVAL_MS = 50L

        /**
         * @param synchronizeWithUiIdle set to false for TalkBack-enabled tests, where the service
         * can continuously emit events and prevent Android from reaching a global idle state.
         */
        fun create(synchronizeWithUiIdle: Boolean = true): AndroidAccessibilityTestDriver {
            return AndroidAccessibilityTestDriver(
                instrumentation = InstrumentationRegistry.getInstrumentation(),
                synchronizeWithUiIdle = synchronizeWithUiIdle,
            )
        }
    }
}
