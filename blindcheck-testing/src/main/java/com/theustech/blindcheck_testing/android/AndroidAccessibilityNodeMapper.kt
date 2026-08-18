package com.theustech.blindcheck_testing.android

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.theustech.blindcheck_testing.model.A11yNodeSnapshot
import com.theustech.blindcheck_testing.model.RectSnapshot

class AndroidAccessibilityNodeMapper(
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val maxNodes: Int = DEFAULT_MAX_NODES,
) {
    fun map(root: AccessibilityNodeInfo?): A11yNodeSnapshot? {
        if (root == null) return null

        val remainingNodes = RemainingNodes(maxNodes)
        return mapNode(root, depth = 0, remainingNodes = remainingNodes)
    }

    private fun mapNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        remainingNodes: RemainingNodes,
    ): A11yNodeSnapshot? {
        if (!remainingNodes.tryConsume()) return null

        val children = node.childSnapshots(depth, remainingNodes)
        return A11yNodeSnapshot(
            text = node.text.normalizedString(),
            contentDescription = node.contentDescription.normalizedString(),
            className = node.className.normalizedString(),
            viewIdResourceName = node.viewIdResourceName.normalizedString(),
            packageName = node.packageName.normalizedString(),
            clickable = node.isClickable,
            enabled = node.isEnabled,
            focused = node.isFocused || node.isAccessibilityFocused,
            selected = node.isSelected,
            checked = node.isChecked,
            editable = node.isEditable,
            actions = node.actionList.map(AccessibilityActionNames::nameFor),
            boundsInScreen = node.boundsSnapshot(),
            children = children,
        )
    }

    private fun AccessibilityNodeInfo.childSnapshots(
        depth: Int,
        remainingNodes: RemainingNodes,
    ): List<A11yNodeSnapshot> {
        if (depth >= maxDepth) return emptyList()

        return buildList {
            for (index in 0 until childCount) {
                val child = runCatching { getChild(index) }.getOrNull() ?: continue
                try {
                    mapNode(child, depth + 1, remainingNodes)?.let(::add)
                } finally {
                    child.recycle()
                }
            }
        }
    }

    private fun AccessibilityNodeInfo.boundsSnapshot(): RectSnapshot {
        val rect = Rect()
        getBoundsInScreen(rect)
        return RectSnapshot(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
        )
    }

    private fun CharSequence?.normalizedString(): String? {
        return this?.toString()?.trim()?.takeUnless { it.isBlank() }
    }

    private class RemainingNodes(private var remaining: Int) {
        fun tryConsume(): Boolean {
            if (remaining <= 0) return false
            remaining -= 1
            return true
        }
    }

    companion object {
        // A Compose screen that embeds Android views nests well beyond eight levels. With the
        // previous limit the deep nodes were dropped and assertions reported an empty window
        // instead of the content that was really there.
        const val DEFAULT_MAX_DEPTH = 24
        const val DEFAULT_MAX_NODES = 250
    }
}
