package com.theustech.blindcheck_testing.assertions

import com.theustech.blindcheck_testing.model.A11yEventRecord

data class FocusSequenceExpectation(
    val items: List<FocusExpectation>,
) {
    fun matches(events: List<A11yEventRecord>): Boolean {
        var nextExpectedIndex = 0
        for (event in events) {
            if (nextExpectedIndex >= items.size) {
                return true
            }
            if (items[nextExpectedIndex].matches(event)) {
                nextExpectedIndex += 1
            }
        }
        return nextExpectedIndex == items.size
    }

    fun assertMatches(events: List<A11yEventRecord>, targetPackage: String? = null) {
        var nextExpectedIndex = 0
        for (event in events) {
            if (nextExpectedIndex >= items.size) return
            if (items[nextExpectedIndex].matches(event)) {
                nextExpectedIndex += 1
            }
        }
        if (nextExpectedIndex < items.size) {
            val expected = items[nextExpectedIndex]
            val lastObserved = events.lastOrNull()
            val message = buildString {
                appendLine(
                    "Expected focus sequence item #${nextExpectedIndex + 1} " +
                        "matching [${expected.describe()}], but was not found in the event stream.",
                )
                targetPackage?.let { appendLine("Target package: $it") }
                lastObserved?.let {
                    appendLine("Last event id: ${it.id}")
                    appendLine("Last event timestamp: ${it.timestamp}")
                    appendLine("Last event text: ${it.text}")
                }
            }
            throw AssertionError(message.trimEnd())
        }
    }
}
