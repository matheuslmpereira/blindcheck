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
}
