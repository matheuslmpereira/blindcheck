package com.theustech.blindcheck_tracking_app

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.theustech.blindcheck_tracker.A11yEventNormalizer
import com.theustech.blindcheck_tracker.RemoteActions
import com.theustech.blindcheck_tracker.TrackingEventStore

class TrackingAccessibilityService : AccessibilityService(), ActionExecutor {

    private val normalizer = A11yEventNormalizer()
    private val eventStore = TrackingEventStore.shared

    // Index of the last node we successfully focused in the candidate list.
    // Simpler and more reliable than fingerprinting — avoids className mismatches
    // between event source and tree-traversal nodes (common in Compose).
    @Volatile private var lastFocusedIdx: Int = -1

    override fun onServiceConnected() {
        instance = this
        executor = this
    }

    override fun onDestroy() {
        instance = null
        executor = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        eventStore.record(normalizer.normalize(event))
        // Reset focus index on window transitions so navigation always starts fresh
        // when the user switches apps or screens.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            lastFocusedIdx = -1
        }
    }

    override fun onInterrupt() = Unit

    override fun execute(action: String) {
        Log.d(TAG, "Executing remote action: $action")
        when (action) {
            RemoteActions.ACTION_NEXT -> moveFocus(forward = true)
            RemoteActions.ACTION_PREVIOUS -> moveFocus(forward = false)
            RemoteActions.ACTION_ACTIVATE -> activateFocused()
            RemoteActions.ACTION_BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            RemoteActions.ACTION_SCROLL_FORWARD -> scrollFocused(forward = true)
            RemoteActions.ACTION_SCROLL_BACKWARD -> scrollFocused(forward = false)
            RemoteActions.ACTION_HOME -> performGlobalAction(GLOBAL_ACTION_HOME)
            RemoteActions.ACTION_RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        }
    }

    private fun moveFocus(forward: Boolean) {
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo, filter: (AccessibilityNodeInfo) -> Boolean) {
            if (filter(node)) {
                candidates.add(AccessibilityNodeInfo.obtain(node))
                return
            }
            for (i in 0 until node.childCount) {
                val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                try { traverse(child, filter) } finally { child.recycle() }
            }
        }

        val currentIdx = lastFocusedIdx

        try {
            // Pass 1: isScreenReaderFocusable — Compose apps use this to exclude merged
            // children (e.g. TextField label) from independent screen-reader focus.
            (rootInActiveWindow ?: return).useNode { traverse(it, ::isScreenReaderFocusable) }

            // Pass 2: system/traditional apps don't set isScreenReaderFocusable, so fall
            // back to ACTION_ACCESSIBILITY_FOCUS + content heuristic.
            if (candidates.isEmpty()) {
                (rootInActiveWindow ?: return).useNode { traverse(it, ::isActionFocusable) }
            }

            if (candidates.isEmpty()) {
                Log.w(TAG, "moveFocus: no focusable candidates found")
                return
            }

            candidates.forEachIndexed { i, n ->
                Log.v(TAG, "  candidate[${i + 1}/${candidates.size}] " +
                    "class=${n.className?.toString()?.substringAfterLast('.')} " +
                    "text='${n.text}' desc='${n.contentDescription}' " +
                    "important=${n.isImportantForAccessibility}")
            }

            // Use stored index directly — no fingerprint comparison needed
            val focusedIdx = if (currentIdx < 0 || currentIdx >= candidates.size) -1 else currentIdx

            Log.d(TAG, "moveFocus(forward=$forward) candidates=${candidates.size} storedIdx=$currentIdx focusedIdx=$focusedIdx")

            val targetIdx = when {
                forward && focusedIdx < 0 -> 0
                forward -> (focusedIdx + 1).coerceAtMost(candidates.size - 1)
                focusedIdx <= 0 -> candidates.size - 1
                else -> focusedIdx - 1
            }
            val target = candidates[targetIdx]
            val ok = target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            if (ok) lastFocusedIdx = targetIdx
            Log.d(TAG, "Focus[${targetIdx + 1}/${candidates.size}] ok=$ok " +
                "class=${target.className?.toString()?.substringAfterLast('.')} " +
                "text='${target.text}' " +
                "desc='${target.contentDescription}' " +
                "editable=${target.isEditable} " +
                "clickable=${target.isClickable} " +
                "password=${target.isPassword}"
            )
        } finally {
            candidates.forEach { runCatching { it.recycle() } }
        }
    }

    private fun activateFocused() {
        val root = rootInActiveWindow ?: return
        root.useNode { node ->
            (node.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT))
                ?.useNode { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
        }
    }

    private fun scrollFocused(forward: Boolean) {
        val root = rootInActiveWindow ?: return
        root.useNode { node ->
            findFirstScrollable(node)?.useNode { scrollable ->
                val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                             else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                scrollable.performAction(action)
            }
        }
    }

    // For Compose apps: respects semantic merging (excludes e.g. TextField labels merged into parent).
    private fun isScreenReaderFocusable(node: AccessibilityNodeInfo): Boolean =
        node.isVisibleToUser && AccessibilityNodeInfoCompat.wrap(node).isScreenReaderFocusable

    // For system/traditional apps: ACTION_ACCESSIBILITY_FOCUS + content heuristic.
    private fun isActionFocusable(node: AccessibilityNodeInfo): Boolean =
        node.isVisibleToUser &&
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS } &&
            (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank() ||
                node.isEditable || node.isClickable)

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return AccessibilityNodeInfo.obtain(node)
        for (i in 0 until node.childCount) {
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            val result = child.useNode { findFirstScrollable(it) }
            if (result != null) return result
        }
        return null
    }

    private inline fun <T> AccessibilityNodeInfo.useNode(block: (AccessibilityNodeInfo) -> T): T {
        return try { block(this) } finally { recycle() }
    }

    companion object {
        private const val TAG = "BlindCheckTracker"

        @Volatile
        var instance: TrackingAccessibilityService? = null
            private set

        @Volatile
        internal var executor: ActionExecutor? = null

        fun setExecutorForTest(e: ActionExecutor?) {
            executor = e
        }
    }
}
