package com.theustech.blindcheck_tracking_app.data

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityEventTypeTest {
    @Test
    fun entriesMatchAndroidEventTypeNames() {
        AccessibilityEventType.entries.forEach { eventType ->
            assertEquals(
                eventType.androidName,
                AccessibilityEvent.eventTypeToString(eventType.androidValue),
            )
        }
    }

    @Test
    fun fromAndroidNameMatchesDocumentedEventName() {
        assertEquals(
            AccessibilityEventType.ViewAccessibilityFocused,
            AccessibilityEventType.fromAndroidName("TYPE_VIEW_ACCESSIBILITY_FOCUSED"),
        )
    }

    @Test
    fun fromAndroidNameReturnsNullForUnknownEventName() {
        assertNull(AccessibilityEventType.fromAndroidName("TYPE_UNKNOWN"))
        assertNull(AccessibilityEventType.fromAndroidName(null))
    }
}
