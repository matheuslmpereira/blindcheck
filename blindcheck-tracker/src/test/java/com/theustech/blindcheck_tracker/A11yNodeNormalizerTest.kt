package com.theustech.blindcheck_tracker

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.theustech.blindcheck_testing.model.RectSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class A11yNodeNormalizerTest {
    @Test
    fun normalize_extractsFlagsBoundsAndActions() {
        val node = AccessibilityNodeInfo.obtain().apply {
            text = "E-mail"
            contentDescription = "Campo de e-mail"
            className = "android.widget.EditText"
            viewIdResourceName = "com.example.app:id/email"
            packageName = "com.example.app"
            isClickable = true
            isEnabled = true
            isFocusable = true
            isFocused = true
            isSelected = true
            isChecked = true
            isEditable = true
            setBoundsInScreen(Rect(1, 2, 30, 40))
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
        }

        val snapshot = A11yNodeNormalizer().normalize(node)

        assertEquals("E-mail", snapshot.text)
        assertEquals("Campo de e-mail", snapshot.contentDescription)
        assertEquals("android.widget.EditText", snapshot.className)
        assertEquals("com.example.app:id/email", snapshot.viewIdResourceName)
        assertEquals("com.example.app", snapshot.packageName)
        assertTrue(snapshot.clickable)
        assertTrue(snapshot.enabled)
        assertTrue(snapshot.focused)
        assertTrue(snapshot.selected)
        assertTrue(snapshot.checked)
        assertTrue(snapshot.editable)
        assertEquals(RectSnapshot(left = 1, top = 2, right = 30, bottom = 40), snapshot.boundsInScreen)
        assertTrue(snapshot.actions.contains("ACTION_CLICK"))
        assertTrue(snapshot.actions.contains("ACTION_SET_TEXT"))
    }

    @Test
    fun normalize_mapsEmptyTextAndContentDescriptionToNull() {
        val node = AccessibilityNodeInfo.obtain().apply {
            text = ""
            contentDescription = ""
        }

        val snapshot = A11yNodeNormalizer().normalize(node)

        assertEquals(null, snapshot.text)
        assertEquals(null, snapshot.contentDescription)
        assertFalse(snapshot.clickable)
    }

    @Test
    fun normalize_respectsDepthLimit() {
        val root = AccessibilityNodeInfo.obtain().apply { text = "root" }
        val child = AccessibilityNodeInfo.obtain().apply { text = "child" }
        val grandchild = AccessibilityNodeInfo.obtain().apply { text = "grandchild" }
        Shadows.shadowOf(child).addChild(grandchild)
        Shadows.shadowOf(root).addChild(child)

        val snapshot = A11yNodeNormalizer(maxDepth = 1, maxNodes = 10).normalize(root)

        assertEquals(listOf("child"), snapshot.children.map { it.text })
        assertTrue(snapshot.children.single().children.isEmpty())
    }

    @Test
    fun normalize_respectsNodeLimitWhilePreservingOrder() {
        val root = AccessibilityNodeInfo.obtain().apply { text = "root" }
        Shadows.shadowOf(root).addChild(AccessibilityNodeInfo.obtain().apply { text = "first" })
        Shadows.shadowOf(root).addChild(AccessibilityNodeInfo.obtain().apply { text = "second" })
        Shadows.shadowOf(root).addChild(AccessibilityNodeInfo.obtain().apply { text = "third" })

        val snapshot = A11yNodeNormalizer(maxDepth = 2, maxNodes = 3).normalize(root)

        assertEquals(listOf("first", "second"), snapshot.children.map { it.text })
    }
}
