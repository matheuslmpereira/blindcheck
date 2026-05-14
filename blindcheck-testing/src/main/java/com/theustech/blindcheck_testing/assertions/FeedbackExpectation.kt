package com.theustech.blindcheck_testing.assertions

import com.theustech.blindcheck_testing.model.A11yEventRecord

data class FeedbackExpectation(
    val contains: String,
) {
    fun matches(event: A11yEventRecord): Boolean {
        val candidates = event.text + listOfNotNull(event.contentDescription)
        return candidates.any { it.contains(contains) }
    }
}
