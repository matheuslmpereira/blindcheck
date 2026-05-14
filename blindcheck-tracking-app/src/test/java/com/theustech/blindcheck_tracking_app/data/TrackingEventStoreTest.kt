package com.theustech.blindcheck_tracking_app.data

import com.theustech.blindcheck_testing.model.A11yEventRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingEventStoreTest {
    @Test
    fun record_recordsAllPackagesByDefault() {
        val store = TrackingEventStore()

        store.record(event("first", packageName = "com.first"))
        store.record(event("second", packageName = "com.second"))
        store.record(event("missing-package", packageName = null))

        assertEquals(listOf("first", "second", "missing-package"), store.snapshot().map { it.id })
        assertEquals(listOf("com.first", "com.second"), store.observedPackagesSnapshot())
    }

    @Test
    fun record_preservesInsertionOrder() {
        val store = TrackingEventStore()

        store.record(event("first", packageName = "com.example.app"))
        store.record(event("second", packageName = "com.example.app"))
        store.record(event("third", packageName = "com.example.app"))

        assertEquals(listOf("first", "second", "third"), store.snapshot().map { it.id })
    }

    @Test
    fun record_appliesTargetPackageFilter() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.target")

        store.record(event("ignored", packageName = "com.other"))
        store.record(event("kept", packageName = "com.target"))
        store.record(event("null-package", packageName = null))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
        assertEquals(listOf("com.other", "com.target"), store.observedPackagesSnapshot())
    }

    @Test
    fun record_appliesTargetEventTypeFilter() {
        val store = TrackingEventStore()
        store.addTargetEventType(AccessibilityEventType.ViewAccessibilityFocused)

        store.record(event("ignored", eventType = "TYPE_VIEW_CLICKED"))
        store.record(event("kept", eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED"))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
        assertEquals(listOf(AccessibilityEventType.ViewAccessibilityFocused), store.targetEventTypesSnapshot())
    }

    @Test
    fun record_appliesPackageAndEventTypeFiltersTogether() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.target")
        store.addTargetEventType(AccessibilityEventType.ViewAccessibilityFocused)

        store.record(
            event(
                id = "ignored-package",
                packageName = "com.other",
                eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
            ),
        )
        store.record(
            event(
                id = "ignored-event-type",
                packageName = "com.target",
                eventType = "TYPE_WINDOW_CONTENT_CHANGED",
            ),
        )
        store.record(
            event(
                id = "kept",
                packageName = "com.target",
                eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
            ),
        )

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
    }

    @Test
    fun record_dropsMissingPackageEventsWhenCaptureFilterIsActive() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.target")

        store.record(event("missing-package", packageName = null))
        store.record(event("kept", packageName = "com.target"))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
    }

    @Test
    fun addTargetPackage_ignoresBlankPackages() {
        val store = TrackingEventStore()

        store.addTargetPackage(null)
        store.addTargetPackage("")
        store.addTargetPackage("  ")

        assertTrue(store.targetPackagesSnapshot().isEmpty())
    }

    @Test
    fun addTargetPackage_deduplicatesPackages() {
        val store = TrackingEventStore()

        store.addTargetPackage("com.target")
        store.addTargetPackage("com.target")

        assertEquals(listOf("com.target"), store.targetPackagesSnapshot())
    }

    @Test
    fun addTargetEventType_ignoresNullEventTypes() {
        val store = TrackingEventStore()

        store.addTargetEventType(null)

        assertTrue(store.targetEventTypesSnapshot().isEmpty())
    }

    @Test
    fun addTargetEventType_deduplicatesEventTypes() {
        val store = TrackingEventStore()

        store.addTargetEventType(AccessibilityEventType.ViewAccessibilityFocused)
        store.addTargetEventType(AccessibilityEventType.ViewAccessibilityFocused)

        assertEquals(listOf(AccessibilityEventType.ViewAccessibilityFocused), store.targetEventTypesSnapshot())
    }

    @Test
    fun record_appliesMultipleTargetPackageFilters() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.first")
        store.addTargetPackage("com.second")

        store.record(event("first", packageName = "com.first"))
        store.record(event("second", packageName = "com.second"))
        store.record(event("ignored", packageName = "com.third"))

        assertEquals(listOf("first", "second"), store.snapshot().map { it.id })
        assertEquals(listOf("com.first", "com.second"), store.targetPackagesSnapshot())
    }

    @Test
    fun removeTargetPackage_updatesFiltering() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.first")
        store.addTargetPackage("com.second")
        store.removeTargetPackage("com.first")

        store.record(event("ignored", packageName = "com.first"))
        store.record(event("kept", packageName = "com.second"))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
        assertEquals(listOf("com.second"), store.targetPackagesSnapshot())
    }

    @Test
    fun clearTargetPackages_returnsToShowingAllEvents() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.first")
        store.record(event("first", packageName = "com.first"))
        store.record(event("ignored", packageName = "com.second"))

        assertEquals(listOf("first"), store.snapshot().map { it.id })

        store.clearTargetPackages()
        store.record(event("second", packageName = "com.second"))

        assertEquals(listOf("first", "second"), store.snapshot().map { it.id })
        assertTrue(store.targetPackagesSnapshot().isEmpty())
    }

    @Test
    fun clearTargetEventTypes_returnsToShowingAllEventTypes() {
        val store = TrackingEventStore()
        store.addTargetEventType(AccessibilityEventType.ViewAccessibilityFocused)
        store.record(event("first", eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED"))
        store.record(event("ignored", eventType = "TYPE_VIEW_CLICKED"))

        assertEquals(listOf("first"), store.snapshot().map { it.id })

        store.clearTargetEventTypes()
        store.record(event("second", eventType = "TYPE_VIEW_CLICKED"))

        assertEquals(listOf("first", "second"), store.snapshot().map { it.id })
        assertTrue(store.targetEventTypesSnapshot().isEmpty())
    }

    @Test
    fun record_ignoresEventsWhenRecordingIsOff() {
        val store = TrackingEventStore()
        store.stopRecording()

        store.record(event("ignored"))
        store.startRecording()
        store.record(event("kept"))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
    }

    @Test
    fun clear_removesRecordedEvents() {
        val store = TrackingEventStore()
        store.record(event("first"))
        store.record(event("second"))

        store.clear()

        assertTrue(store.snapshot().isEmpty())
    }

    @Test
    fun clear_resetsObservedPackagesAndTargetPackages() {
        val store = TrackingEventStore()
        store.addTargetPackage("com.first")
        store.addTargetEventType(AccessibilityEventType.ViewAccessibilityFocused)
        store.record(event("first", packageName = "com.first"))
        store.record(event("second", packageName = "com.second"))

        store.clear()

        assertTrue(store.observedPackagesSnapshot().isEmpty())
        assertTrue(store.targetPackagesSnapshot().isEmpty())
        assertTrue(store.targetEventTypesSnapshot().isEmpty())
    }

    @Test
    fun snapshot_returnsCopy() {
        val store = TrackingEventStore()
        store.record(event("first"))

        val snapshot = store.snapshot().toMutableList()
        snapshot.clear()

        assertEquals(listOf("first"), store.snapshot().map { it.id })
    }

    private fun event(
        id: String,
        packageName: String? = "com.example.app",
        eventType: String = "TYPE_VIEW_CLICKED",
    ): A11yEventRecord {
        return A11yEventRecord(
            id = id,
            timestamp = 1L,
            packageName = packageName,
            eventType = eventType,
        )
    }
}
