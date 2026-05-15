package com.theustech.blindcheck_testing.android

import android.view.accessibility.AccessibilityNodeInfo

internal object AccessibilityActionNames {
    fun nameFor(action: AccessibilityNodeInfo.AccessibilityAction): String {
        val knownName = knownActionNames[action.id]
        if (knownName != null) return knownName

        return action.label?.toString()?.takeIf { it.isNotBlank() }
            ?: "ACTION_${action.id}"
    }

    private val knownActionNames = mapOf(
        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS to "ACTION_ACCESSIBILITY_FOCUS",
        AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS to "ACTION_CLEAR_ACCESSIBILITY_FOCUS",
        AccessibilityNodeInfo.ACTION_CLEAR_FOCUS to "ACTION_CLEAR_FOCUS",
        AccessibilityNodeInfo.ACTION_CLEAR_SELECTION to "ACTION_CLEAR_SELECTION",
        AccessibilityNodeInfo.ACTION_CLICK to "ACTION_CLICK",
        AccessibilityNodeInfo.ACTION_COLLAPSE to "ACTION_COLLAPSE",
        AccessibilityNodeInfo.ACTION_COPY to "ACTION_COPY",
        AccessibilityNodeInfo.ACTION_CUT to "ACTION_CUT",
        AccessibilityNodeInfo.ACTION_DISMISS to "ACTION_DISMISS",
        AccessibilityNodeInfo.ACTION_EXPAND to "ACTION_EXPAND",
        AccessibilityNodeInfo.ACTION_FOCUS to "ACTION_FOCUS",
        AccessibilityNodeInfo.ACTION_LONG_CLICK to "ACTION_LONG_CLICK",
        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY to "ACTION_NEXT_AT_MOVEMENT_GRANULARITY",
        AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT to "ACTION_NEXT_HTML_ELEMENT",
        AccessibilityNodeInfo.ACTION_PASTE to "ACTION_PASTE",
        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY to "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY",
        AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT to "ACTION_PREVIOUS_HTML_ELEMENT",
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD to "ACTION_SCROLL_BACKWARD",
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD to "ACTION_SCROLL_FORWARD",
        AccessibilityNodeInfo.ACTION_SELECT to "ACTION_SELECT",
        AccessibilityNodeInfo.ACTION_SET_SELECTION to "ACTION_SET_SELECTION",
        AccessibilityNodeInfo.ACTION_SET_TEXT to "ACTION_SET_TEXT",
    )
}
