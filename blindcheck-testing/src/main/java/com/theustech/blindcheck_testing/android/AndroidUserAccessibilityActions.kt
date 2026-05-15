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
        unsupported("next")
    }

    override suspend fun previous() {
        unsupported("previous")
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
        unsupported("scrollForward")
    }

    override suspend fun scrollBackward() {
        unsupported("scrollBackward")
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

    private fun findFocusedNode(): AccessibilityNodeInfo? {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return null
        return root.useNode { node ->
            node.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: node.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }
    }

    private fun unsupported(actionName: String): Nothing {
        throw UnsupportedOperationException(
            "UserAccessibilityActions.$actionName is not implemented in this BlindCheck testing slice. " +
                "This slice supports activate, inputText, and back only.",
        )
    }

    private inline fun <T> AccessibilityNodeInfo.useNode(block: (AccessibilityNodeInfo) -> T): T {
        return try {
            block(this)
        } finally {
            recycle()
        }
    }
}
