package com.theustech.blindcheck_tracking_app

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.theustech.blindcheck_tracker.A11yEventNormalizer
import com.theustech.blindcheck_tracker.RemoteActions
import com.theustech.blindcheck_tracker.TrackingEventStore

class TrackingAccessibilityService : AccessibilityService(), ActionExecutor {

    private val normalizer = A11yEventNormalizer()
    private val eventStore = TrackingEventStore.shared

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
        }
    }

    private fun moveFocus(forward: Boolean) {
        val root = rootInActiveWindow ?: return
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo) {
            if (isAccessibilityFocusable(node)) candidates.add(AccessibilityNodeInfo.obtain(node))
            for (i in 0 until node.childCount) {
                val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                try { traverse(child) } finally { child.recycle() }
            }
        }

        try {
            root.useNode { traverse(it) }
            if (candidates.isEmpty()) return
            val focusedIdx = candidates.indexOfFirst { it.isAccessibilityFocused }
            val targetIdx = when {
                forward && focusedIdx < 0 -> 0
                forward -> (focusedIdx + 1).coerceAtMost(candidates.size - 1)
                focusedIdx <= 0 -> candidates.size - 1
                else -> focusedIdx - 1
            }
            candidates[targetIdx].performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
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

    private fun isAccessibilityFocusable(node: AccessibilityNodeInfo): Boolean {
        return !node.text.isNullOrBlank() ||
            !node.contentDescription.isNullOrBlank() ||
            node.isClickable ||
            node.isEditable
    }

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
