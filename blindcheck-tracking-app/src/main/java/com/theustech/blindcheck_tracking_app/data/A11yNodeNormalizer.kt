package com.theustech.blindcheck_tracking_app.data

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.theustech.blindcheck_testing.model.A11yNodeSnapshot
import com.theustech.blindcheck_testing.model.RectSnapshot

class A11yNodeNormalizer(
    private val maxDepth: Int = DEFAULT_MAX_DEPTH,
    private val maxNodes: Int = DEFAULT_MAX_NODES,
) {
    init {
        require(maxDepth >= 0) { "maxDepth must be >= 0" }
        require(maxNodes > 0) { "maxNodes must be > 0" }
    }

    fun normalize(node: AccessibilityNodeInfo): A11yNodeSnapshot {
        val budget = TraversalBudget(maxNodes)
        return normalizeNode(node, depth = 0, budget)
    }

    private fun normalizeNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        budget: TraversalBudget,
    ): A11yNodeSnapshot {
        budget.consume()

        return A11yNodeSnapshot(
            text = node.text.normalizedString(),
            contentDescription = node.contentDescription.normalizedString(),
            className = node.className.normalizedString(),
            viewIdResourceName = node.viewIdResourceName.normalizedString(),
            packageName = node.packageName.normalizedString(),
            clickable = node.isClickable,
            enabled = node.isEnabled,
            focused = node.isAccessibilityFocused || node.isFocused,
            selected = node.isSelected,
            checked = node.isChecked,
            editable = node.isEditable,
            actions = node.normalizedActions(),
            boundsInScreen = node.normalizedBounds(),
            children = node.normalizedChildren(depth, budget),
        )
    }

    private fun AccessibilityNodeInfo.normalizedChildren(
        depth: Int,
        budget: TraversalBudget,
    ): List<A11yNodeSnapshot> {
        if (depth >= maxDepth || !budget.hasRemaining()) return emptyList()

        return buildList {
            for (index in 0 until childCount) {
                if (!budget.hasRemaining()) return@buildList
                val child = runCatching { getChild(index) }.getOrNull() ?: continue
                try {
                    add(normalizeNode(child, depth + 1, budget))
                } finally {
                    child.recycle()
                }
            }
        }
    }

    private fun AccessibilityNodeInfo.normalizedBounds(): RectSnapshot {
        val rect = Rect()
        getBoundsInScreen(rect)
        return RectSnapshot(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
        )
    }

    private fun AccessibilityNodeInfo.normalizedActions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            actionList.map { action -> actionName(action.id, action.label) }
        } else {
            actionBitmaskNames(actions)
        }
    }

    private class TraversalBudget(private var remaining: Int) {
        fun hasRemaining(): Boolean = remaining > 0

        fun consume() {
            if (remaining <= 0) {
                error("Accessibility node traversal budget exhausted")
            }
            remaining -= 1
        }
    }

    companion object {
        const val DEFAULT_MAX_DEPTH = 8
        const val DEFAULT_MAX_NODES = 200
    }
}
