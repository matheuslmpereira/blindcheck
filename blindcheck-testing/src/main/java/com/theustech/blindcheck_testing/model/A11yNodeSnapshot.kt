package com.theustech.blindcheck_testing.model

data class A11yNodeSnapshot(
    val text: String? = null,
    val contentDescription: String? = null,
    val className: String? = null,
    val viewIdResourceName: String? = null,
    val packageName: String? = null,
    val clickable: Boolean = false,
    val enabled: Boolean = false,
    val focused: Boolean = false,
    val selected: Boolean = false,
    val checked: Boolean = false,
    val editable: Boolean = false,
    val actions: List<String> = emptyList(),
    val boundsInScreen: RectSnapshot = RectSnapshot(),
    val children: List<A11yNodeSnapshot> = emptyList(),
)
