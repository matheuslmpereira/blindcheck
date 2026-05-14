package com.theustech.blindcheck_tracking_app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.theustech.blindcheck_tracking_app.data.A11yEventNormalizer
import com.theustech.blindcheck_tracking_app.data.TrackingEventStore

class TrackingAccessibilityService : AccessibilityService() {

    private val normalizer = A11yEventNormalizer()
    private val eventStore = TrackingEventStore.shared

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val record = normalizer.normalize(event)
        eventStore.record(record)
    }

    override fun onInterrupt() = Unit
}
