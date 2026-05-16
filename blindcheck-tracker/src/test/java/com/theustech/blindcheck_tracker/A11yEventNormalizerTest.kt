package com.theustech.blindcheck_tracker

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class A11yEventNormalizerTest {
    private val normalizer = A11yEventNormalizer(
        idProvider = { "event-1" },
    )

    @Test
    fun normalize_handlesNullSourceAndEmptyTextDefensively() {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED).apply {
            eventTime = 123L
            packageName = ""
            className = ""
            contentDescription = ""
            text.add("")
        }

        val record = normalizer.normalize(event)

        assertEquals("event-1", record.id)
        assertEquals(123L, record.timestamp)
        assertEquals("TYPE_VIEW_CLICKED", record.eventType)
        assertNull(record.packageName)
        assertNull(record.className)
        assertEquals(emptyList<String>(), record.text)
        assertNull(record.contentDescription)
        assertNull(record.sourceNode)
    }

    @Test
    fun normalize_treatsBlankStringsAsNull() {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_CLICKED).apply {
            eventTime = 1L
            packageName = "   "
            contentDescription = "  "
            text.add("  ")
        }

        val record = normalizer.normalize(event)

        assertNull("blank packageName should be null", record.packageName)
        assertNull("blank contentDescription should be null", record.contentDescription)
        assertEquals(emptyList<String>(), record.text)
    }

    @Test
    fun normalize_extractsPackageClassEventTypeTextAndContentDescription() {
        val source = AccessibilityNodeInfo.obtain().apply {
            text = "Entrar"
        }
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED).apply {
            eventTime = 456L
            packageName = "com.example.app"
            className = "android.widget.Button"
            contentDescription = "Botao entrar"
            text.add("Entrar")
        }
        Shadows.shadowOf(event).setSourceNode(source)

        val record = normalizer.normalize(event)

        assertEquals("com.example.app", record.packageName)
        assertEquals("android.widget.Button", record.className)
        assertEquals("TYPE_VIEW_ACCESSIBILITY_FOCUSED", record.eventType)
        assertEquals(listOf("Entrar"), record.text)
        assertEquals("Botao entrar", record.contentDescription)
        assertEquals("Entrar", record.sourceNode?.text)
        assertTrue(record.sourceNode?.children.orEmpty().isEmpty())
    }
}
