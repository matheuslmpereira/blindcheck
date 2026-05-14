package com.theustech.blindcheck_tracking_app.data

import android.view.accessibility.AccessibilityEvent
import com.theustech.blindcheck_testing.model.A11yEventRecord
import java.util.UUID

class A11yEventNormalizer(
    private val nodeNormalizer: A11yNodeNormalizer = A11yNodeNormalizer(),
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) {
    fun normalize(event: AccessibilityEvent): A11yEventRecord {
        return A11yEventRecord(
            id = idProvider(),
            timestamp = event.eventTime,
            packageName = event.packageName.normalizedString(),
            eventType = AccessibilityEvent.eventTypeToString(event.eventType),
            className = event.className.normalizedString(),
            text = event.text.mapNotNull { it.normalizedString() },
            contentDescription = event.contentDescription.normalizedString(),
            sourceNode = normalizeSourceNode(event),
        )
    }

    private fun normalizeSourceNode(event: AccessibilityEvent) = runCatching {
        event.source?.let(nodeNormalizer::normalize)
    }.getOrNull()
}
