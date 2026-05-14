package com.theustech.blindcheck_tracking_app.data

import com.theustech.blindcheck_testing.model.A11yEventRecord

class TrackingEventStore {
    private val recordedEvents = mutableListOf<A11yEventRecord>()
    // Session-scoped history for the filter dropdown. Reset through clear().
    private val observedPackages = linkedSetOf<String>()
    private val targetPackages = linkedSetOf<String>()
    private val targetEventTypes = linkedSetOf<AccessibilityEventType>()

    @Volatile
    var isRecording: Boolean = true
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
    fun addTargetPackage(packageName: String?) {
        packageName.normalizedPackageName()?.let(targetPackages::add)
    }

    @Synchronized
    fun removeTargetPackage(packageName: String) {
        targetPackages.remove(packageName)
    }

    @Synchronized
    fun clearTargetPackages() {
        targetPackages.clear()
    }

    @Synchronized
    fun addTargetEventType(eventType: AccessibilityEventType?) {
        eventType?.let(targetEventTypes::add)
    }

    @Synchronized
    fun removeTargetEventType(eventType: AccessibilityEventType) {
        targetEventTypes.remove(eventType)
    }

    @Synchronized
    fun clearTargetEventTypes() {
        targetEventTypes.clear()
    }

    @Synchronized
    fun record(event: A11yEventRecord) {
        if (!isRecording) return
        val packageName = event.packageName.normalizedPackageName()
        if (packageName != null) {
            observedPackages += packageName
        }
        if (targetPackages.isNotEmpty() && packageName !in targetPackages) return
        recordedEvents += event
    }

    @Synchronized
    fun clear() {
        recordedEvents.clear()
        observedPackages.clear()
        targetPackages.clear()
        targetEventTypes.clear()
    }

    @Synchronized
    fun snapshot(): List<A11yEventRecord> {
        return if (targetEventTypes.isEmpty()) {
            recordedEvents.toList()
        } else {
            recordedEvents.filter { matchesTargetEventTypes(it.eventType) }
        }
    }

    @Synchronized
    fun observedPackagesSnapshot(): List<String> = observedPackages.toList()

    @Synchronized
    fun targetPackagesSnapshot(): List<String> = targetPackages.toList()

    @Synchronized
    fun targetEventTypesSnapshot(): List<AccessibilityEventType> = targetEventTypes.toList()

    private fun String?.normalizedPackageName(): String? {
        return this?.trim()?.takeUnless { it.isBlank() }
    }

    private fun matchesTargetEventTypes(eventType: String): Boolean {
        return targetEventTypes.isEmpty() ||
            AccessibilityEventType.fromAndroidName(eventType) in targetEventTypes
    }

    companion object {
        val shared = TrackingEventStore()
    }
}
