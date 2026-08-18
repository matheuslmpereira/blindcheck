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

    private val displayMetrics get() = instrumentation.context.resources.displayMetrics

    override suspend fun next() = swipeHorizontal(forward = true)

    override suspend fun previous() = swipeHorizontal(forward = false)

    // Double tap at screen center — TalkBack activates the currently focused element.
    override suspend fun activate() {
        val cx = displayMetrics.widthPixels / 2
        val cy = displayMetrics.heightPixels / 2
        uiDevice.click(cx, cy)
        Thread.sleep(50)
        uiDevice.click(cx, cy)
    }

    // Single-finger upward swipe — TalkBack passes two-pointer-equivalent scroll through.
    override suspend fun scrollForward() = swipeVertical(up = true)

    override suspend fun scrollBackward() = swipeVertical(up = false)

    // Text input has no natural touch equivalent — ACTION_SET_TEXT is the correct API.
    override suspend fun inputText(value: String) {
        val root = instrumentation.blindCheckUiAutomation().rootInActiveWindow ?: return
        val focused = root.useNode { node ->
            node.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } ?: throw IllegalStateException("Cannot input text because no focused accessibility node was found.")

        focused.useNode { node ->
            if (!node.isEditable) {
                throw IllegalStateException("Focused accessibility node is not editable.")
            }
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
            }
            val textSet = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (!textSet) {
                throw IllegalStateException("Focused editable accessibility node did not accept ACTION_SET_TEXT.")
            }
        }
    }

    override suspend fun back() { uiDevice.pressBack() }

    // TalkBack interprets right swipe as "next", left swipe as "previous".
    private fun swipeHorizontal(forward: Boolean) {
        val w  = displayMetrics.widthPixels
        val cy = displayMetrics.heightPixels / 2
        val (fromX, toX) = if (forward) Pair(w / 4, 3 * w / 4) else Pair(3 * w / 4, w / 4)
        uiDevice.swipe(fromX, cy, toX, cy, SWIPE_STEPS_FAST)
    }

    private fun swipeVertical(up: Boolean) {
        val cx = displayMetrics.widthPixels / 2
        val h  = displayMetrics.heightPixels
        val (fromY, toY) = if (up) Pair((h * 0.7).toInt(), (h * 0.3).toInt())
                           else     Pair((h * 0.3).toInt(), (h * 0.7).toInt())
        uiDevice.swipe(cx, fromY, cx, toY, SWIPE_STEPS_SCROLL)
    }

    companion object {
        private const val SWIPE_STEPS_FAST   = 10  // ~150 ms — navigation swipe
        private const val SWIPE_STEPS_SCROLL = 20  // ~300 ms — scroll swipe
    }
}
