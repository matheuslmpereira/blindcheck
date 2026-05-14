package com.theustech.blindcheck_tracking_app.data

import com.theustech.blindcheck_testing.model.A11yEventRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingEventStoreTest {
    @Test
    fun record_preservesInsertionOrder() {
        val store = TrackingEventStore()
        store.startRecording()

        store.record(event("first", packageName = "com.example.app"))
        store.record(event("second", packageName = "com.example.app"))
        store.record(event("third", packageName = "com.example.app"))

        assertEquals(listOf("first", "second", "third"), store.snapshot().map { it.id })
    }

    @Test
    fun record_appliesTargetPackageFilter() {
        val store = TrackingEventStore()
        store.startRecording()
        store.setTargetPackage("com.target")

        store.record(event("ignored", packageName = "com.other"))
        store.record(event("kept", packageName = "com.target"))
        store.record(event("null-package", packageName = null))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
    }

    @Test
    fun record_ignoresEventsWhenRecordingIsOff() {
        val store = TrackingEventStore()

        store.record(event("ignored"))
        store.startRecording()
        store.record(event("kept"))

        assertEquals(listOf("kept"), store.snapshot().map { it.id })
    }

    @Test
    fun clear_removesRecordedEvents() {
        val store = TrackingEventStore()
        store.startRecording()
        store.record(event("first"))
        store.record(event("second"))

        store.clear()

        assertTrue(store.snapshot().isEmpty())
    }

    @Test
    fun snapshot_returnsCopy() {
        val store = TrackingEventStore()
        store.startRecording()
        store.record(event("first"))

        val snapshot = store.snapshot().toMutableList()
        snapshot.clear()

        assertEquals(listOf("first"), store.snapshot().map { it.id })
    }

    @Test
    fun list_returnsCurrentSnapshot() {
        val store = TrackingEventStore()
        store.startRecording()

        store.record(event("first"))

        assertEquals(store.snapshot(), store.list())
    }

    private fun event(
        id: String,
        packageName: String? = "com.example.app",
    ): A11yEventRecord {
        return A11yEventRecord(
            id = id,
            timestamp = 1L,
            packageName = packageName,
            eventType = "TYPE_VIEW_CLICKED",
        )
    }
}
