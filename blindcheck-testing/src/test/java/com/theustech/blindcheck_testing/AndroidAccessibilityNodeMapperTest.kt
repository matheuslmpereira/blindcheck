package com.theustech.blindcheck_testing

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.theustech.blindcheck_testing.android.AndroidAccessibilityNodeMapper
import com.theustech.blindcheck_testing.model.RectSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidAccessibilityNodeMapperTest {
    private val mapper = AndroidAccessibilityNodeMapper()

    @Test
    fun map_returnsNullForNullNode() {
        assertNull(mapper.map(null))
    }

    @Test
    fun map_capturesSerializableNodeState() {
        val node = AccessibilityNodeInfo.obtain().apply {
            text = "Entrar"
            contentDescription = "Botao entrar"
            className = "android.widget.Button"
            viewIdResourceName = "com.example:id/submit"
            packageName = "com.example"
            isClickable = true
            isEnabled = true
            isFocused = true
            isSelected = true
            isChecked = true
            isEditable = true
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
            setBoundsInScreen(Rect(1, 2, 3, 4))
        }

        val snapshot = node.useNode { mapper.map(it) }!!

        assertEquals("Entrar", snapshot.text)
        assertEquals("Botao entrar", snapshot.contentDescription)
        assertEquals("android.widget.Button", snapshot.className)
        assertEquals("com.example:id/submit", snapshot.viewIdResourceName)
        assertEquals("com.example", snapshot.packageName)
        assertTrue(snapshot.clickable)
        assertTrue(snapshot.enabled)
        assertTrue(snapshot.focused)
        assertTrue(snapshot.selected)
        assertTrue(snapshot.checked)
        assertTrue(snapshot.editable)
        assertEquals(RectSnapshot(left = 1, top = 2, right = 3, bottom = 4), snapshot.boundsInScreen)
        assertTrue("expected ACTION_CLICK", "ACTION_CLICK" in snapshot.actions)
        assertTrue("expected ACTION_SET_TEXT", "ACTION_SET_TEXT" in snapshot.actions)
    }

    @Test
    fun map_treatsBlankTextAsNull() {
        val node = AccessibilityNodeInfo.obtain().apply {
            text = " "
            contentDescription = ""
            className = "android.widget.TextView"
        }

        val snapshot = node.useNode { mapper.map(it) }!!

        assertNull(snapshot.text)
        assertNull(snapshot.contentDescription)
        assertEquals("android.widget.TextView", snapshot.className)
    }

    private inline fun <T> AccessibilityNodeInfo.useNode(block: (AccessibilityNodeInfo) -> T): T {
        return try {
            block(this)
        } finally {
            recycle()
        }
    }
}
