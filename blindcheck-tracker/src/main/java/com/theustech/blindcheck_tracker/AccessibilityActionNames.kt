package com.theustech.blindcheck_tracker

import android.view.accessibility.AccessibilityNodeInfo

internal fun actionName(actionId: Int, label: CharSequence?): String {
    return knownActionNames[actionId]
        ?: label.normalizedString()
        ?: "ACTION_$actionId"
}

internal fun actionBitmaskNames(actions: Int): List<String> {
    return knownActionNames
        .filterKeys { actionId -> actions and actionId == actionId }
        .values
        .toList()
}

private val knownActionNames = mapOf(
    AccessibilityNodeInfo.ACTION_FOCUS to "ACTION_FOCUS",
    AccessibilityNodeInfo.ACTION_CLEAR_FOCUS to "ACTION_CLEAR_FOCUS",
    AccessibilityNodeInfo.ACTION_SELECT to "ACTION_SELECT",
    AccessibilityNodeInfo.ACTION_CLEAR_SELECTION to "ACTION_CLEAR_SELECTION",
    AccessibilityNodeInfo.ACTION_CLICK to "ACTION_CLICK",
    AccessibilityNodeInfo.ACTION_LONG_CLICK to "ACTION_LONG_CLICK",
    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS to "ACTION_ACCESSIBILITY_FOCUS",
    AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS to "ACTION_CLEAR_ACCESSIBILITY_FOCUS",
    AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY to "ACTION_NEXT_AT_MOVEMENT_GRANULARITY",
    AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY to "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY",
    AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT to "ACTION_NEXT_HTML_ELEMENT",
    AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT to "ACTION_PREVIOUS_HTML_ELEMENT",
    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD to "ACTION_SCROLL_FORWARD",
    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD to "ACTION_SCROLL_BACKWARD",
    AccessibilityNodeInfo.ACTION_COPY to "ACTION_COPY",
    AccessibilityNodeInfo.ACTION_PASTE to "ACTION_PASTE",
    AccessibilityNodeInfo.ACTION_CUT to "ACTION_CUT",
    AccessibilityNodeInfo.ACTION_SET_SELECTION to "ACTION_SET_SELECTION",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT.id to "ACTION_SET_TEXT",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_TO_POSITION.id to "ACTION_SCROLL_TO_POSITION",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id to "ACTION_SHOW_ON_SCREEN",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP.id to "ACTION_SCROLL_UP",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id to "ACTION_SCROLL_LEFT",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN.id to "ACTION_SCROLL_DOWN",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id to "ACTION_SCROLL_RIGHT",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_CONTEXT_CLICK.id to "ACTION_CONTEXT_CLICK",
    AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id to "ACTION_SET_PROGRESS",
)
