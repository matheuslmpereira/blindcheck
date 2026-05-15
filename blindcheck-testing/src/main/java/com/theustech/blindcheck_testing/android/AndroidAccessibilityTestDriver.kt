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
                .any(expectation::matches)
        }
        if (!matched) {
            throw AssertionError(
                "Expected current accessibility window to contain [${expectation.describe()}], " +
                    "but no matching node was found.",
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
                .any(expectation::matches)
        }
        if (!matched) {
            throw AssertionError(
                "Expected the focused accessibility node to match [${expectation.describe()}], " +
                    "but no focused node matched.",
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

        for (index in 0 until childCount) {
            val child = runCatching { getChild(index) }.getOrNull() ?: continue
            val match = child.useNode { it.findFirstMatching(expectation) }
            if (match != null) return match
        }
        return null
    }

    private fun currentWindowSnapshots(): List<A11yNodeSnapshot> {
        instrumentation.waitForIdleSync()
        return instrumentation.uiAutomation.windows
            .sortedByDescending { it.isActive }
            .mapNotNull { window -> runCatching { window.root }.getOrNull() }
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

    private inline fun <T> AccessibilityNodeInfo.useNode(block: (AccessibilityNodeInfo) -> T): T {
        return try {
            block(this)
        } finally {
            recycle()
        }
    }

    companion object {
        const val SYNTHETIC_SNAPSHOT_EVENT_TYPE = "TYPE_SYNTHETIC_WINDOW_SNAPSHOT"
        private const val DEFAULT_ASSERTION_TIMEOUT_MS = 2_000L
        private const val DEFAULT_ASSERTION_POLL_INTERVAL_MS = 50L

        fun create(): AndroidAccessibilityTestDriver {
            return AndroidAccessibilityTestDriver(
                instrumentation = InstrumentationRegistry.getInstrumentation(),
            )
        }
    }
}
