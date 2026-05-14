package com.theustech.blindcheck_tracking_app.data

import com.theustech.blindcheck_testing.model.A11yEventRecord

class TrackingEventStore {
    private val events = mutableListOf<A11yEventRecord>()

    @Volatile
    var isRecording: Boolean = false
        private set

    @Volatile
    var targetPackage: String? = null
        private set

    @Synchronized
    fun startRecording() {
        isRecording = true
    }

    @Synchronized
    fun stopRecording() {
        isRecording = false
    }

    @Synchronized
    fun setRecording(recording: Boolean) {
        if (recording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    @Synchronized
    fun setTargetPackage(packageName: String?) {
        targetPackage = packageName?.trim()?.takeUnless { it.isBlank() }
    }

    @Synchronized
    fun record(event: A11yEventRecord) {
        if (!isRecording) return
        val filter = targetPackage
        if (filter != null && event.packageName != filter) return
        events += event
    }

    @Synchronized
    fun clear() {
        events.clear()
    }

    @Synchronized
    fun snapshot(): List<A11yEventRecord> = events.toList()

    fun list(): List<A11yEventRecord> = snapshot()

    fun eventsSnapshot(): List<A11yEventRecord> = snapshot()

    companion object {
        val shared = TrackingEventStore()
    }
}
