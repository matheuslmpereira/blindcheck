package com.theustech.blindcheck_tracking_app.data

import com.theustech.blindcheck_testing.model.A11yEventRecord

class TrackingEventStore {
    private val recordedEvents = mutableListOf<A11yEventRecord>()
    private val observedPackages = linkedSetOf<String>()
    private val targetPackages = linkedSetOf<String>()

    @Volatile
    var isRecording: Boolean = true
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
        targetPackages.clear()
        packageName.normalizedPackageName()?.let(targetPackages::add)
        syncPrimaryTargetPackage()
    }

    @Synchronized
    fun addTargetPackage(packageName: String?) {
        packageName.normalizedPackageName()?.let(targetPackages::add)
        syncPrimaryTargetPackage()
    }

    @Synchronized
    fun removeTargetPackage(packageName: String) {
        targetPackages.remove(packageName)
        syncPrimaryTargetPackage()
    }

    @Synchronized
    fun clearTargetPackages() {
        targetPackages.clear()
        syncPrimaryTargetPackage()
    }

    @Synchronized
    fun record(event: A11yEventRecord) {
        if (!isRecording) return
        val packageName = event.packageName.normalizedPackageName()
        if (packageName != null) {
            observedPackages += packageName
        }
        recordedEvents += event
    }

    @Synchronized
    fun clear() {
        recordedEvents.clear()
    }

    @Synchronized
    fun snapshot(): List<A11yEventRecord> {
        if (targetPackages.isEmpty()) return recordedEvents.toList()

        return recordedEvents
            .filter { event -> event.packageName.normalizedPackageName() in targetPackages }
            .toList()
    }

    @Synchronized
    fun observedPackagesSnapshot(): List<String> = observedPackages.toList()

    @Synchronized
    fun targetPackagesSnapshot(): List<String> = targetPackages.toList()

    private fun syncPrimaryTargetPackage() {
        targetPackage = targetPackages.firstOrNull()
    }

    private fun String?.normalizedPackageName(): String? {
        return this?.trim()?.takeUnless { it.isBlank() }
    }

    companion object {
        val shared = TrackingEventStore()
    }
}
