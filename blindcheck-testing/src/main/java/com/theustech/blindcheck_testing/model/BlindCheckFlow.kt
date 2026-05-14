package com.theustech.blindcheck_testing.model

data class BlindCheckFlow(
    val targetPackage: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val schemaVersion: String = SCHEMA_VERSION,
    val events: List<A11yEventRecord> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = "blindcheck-flow-v1"
    }
}
