package com.theustech.blindcheck_testing.android

import android.view.accessibility.AccessibilityNodeInfo

internal inline fun <T> AccessibilityNodeInfo.useNode(block: (AccessibilityNodeInfo) -> T): T =
    try { block(this) } finally { recycle() }
