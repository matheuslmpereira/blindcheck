package com.theustech.blindcheck_testing.android

import android.app.Instrumentation
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.UiDevice
import com.theustech.blindcheck_testing.actions.UserAccessibilityActions

class AndroidUserAccessibilityActions(
    private val instrumentation: Instrumentation,
    private val uiDevice: UiDevice = UiDevice.getInstance(instrumentation),
) : UserAccessibilityActions {
    override suspend fun next() {
        moveFocus(forward = true)
    }

    override suspend fun previous() {
        moveFocus(forward = false)
    }

    override suspend fun activate() {
        val focused = findFocusedNode()
            ?: throw IllegalStateException("Cannot activate because no focused accessibility node was found.")

        focused.useNode { node ->
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked) {
                throw IllegalStateException("Focused accessibility node did not accept ACTION_CLICK.")
            }
        }
    }

    override suspend fun scrollForward() {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return
        root.useNode { node ->
            node.findFirstScrollable()?.useNode { scrollable ->
                scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
        }
    }

    override suspend fun scrollBackward() {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return
        root.useNode { node ->
            node.findFirstScrollable()?.useNode { scrollable ->
                scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            }
        }
    }

    override suspend fun inputText(value: String) {
        val focused = findFocusedNode()
            ?: throw IllegalStateException("Cannot input text because no focused accessibility node was found.")

        focused.useNode { node ->
            if (!node.isEditable) {
                throw IllegalStateException("Focused accessibility node is not editable.")
            }

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    value,
                )
            }
            val textSet = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (!textSet) {
                throw IllegalStateException("Focused editable accessibility node did not accept ACTION_SET_TEXT.")
            }
        }
    }

    override suspend fun back() {
        uiDevice.pressBack()
    }

    private fun moveFocus(forward: Boolean) {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return
        val candidates = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(node: AccessibilityNodeInfo) {
            if (isAccessibilityFocusable(node)) {
                candidates.add(AccessibilityNodeInfo.obtain(node))
            }
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

    private fun isAccessibilityFocusable(node: AccessibilityNodeInfo): Boolean =
        node.isVisibleToUser &&
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS }

    private fun AccessibilityNodeInfo.findFirstScrollable(): AccessibilityNodeInfo? {
        if (isScrollable) return AccessibilityNodeInfo.obtain(this)
        for (i in 0 until childCount) {
            val child = runCatching { getChild(i) }.getOrNull() ?: continue
            val result = child.useNode { it.findFirstScrollable() }
            if (result != null) return result
        }
        return null
    }

    private fun findFocusedNode(): AccessibilityNodeInfo? {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return null
        return root.useNode { node ->
            node.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }
    }

    private inline fun <T> AccessibilityNodeInfo.useNode(block: (AccessibilityNodeInfo) -> T): T {
        return try {
            block(this)
        } finally {
            recycle()
        }
    }
}
