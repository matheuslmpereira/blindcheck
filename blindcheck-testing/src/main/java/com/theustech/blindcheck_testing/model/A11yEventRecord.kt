package com.theustech.blindcheck_testing.model

data class A11yEventRecord(
    val id: String,
    val timestamp: Long,
    val packageName: String? = null,
    val eventType: String,
    val className: String? = null,
    val text: List<String> = emptyList(),
    val contentDescription: String? = null,
    val sourceNode: A11yNodeSnapshot? = null,
)
