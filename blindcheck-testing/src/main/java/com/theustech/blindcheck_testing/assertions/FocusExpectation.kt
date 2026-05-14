package com.theustech.blindcheck_testing.assertions

import com.theustech.blindcheck_testing.model.A11yEventRecord
import com.theustech.blindcheck_testing.model.A11yNodeSnapshot

data class FocusExpectation(
    val textEquals: String? = null,
    val textContains: String? = null,
    val contentDescriptionEquals: String? = null,
    val contentDescriptionContains: String? = null,
    val clickable: Boolean? = null,
    val editable: Boolean? = null,
    val enabled: Boolean? = null,
    val selected: Boolean? = null,
    val checked: Boolean? = null,
) {
    fun matches(node: A11yNodeSnapshot): Boolean {
        return matchesText(node.text) &&
            matchesContentDescription(node.contentDescription) &&
            matchesState(node)
    }

    fun matches(event: A11yEventRecord): Boolean {
        val sourceNode = event.sourceNode
        return matchesEventText(event, sourceNode) &&
            matchesEventContentDescription(event, sourceNode) &&
            matchesEventState(sourceNode)
    }

    private fun matchesEventText(
        event: A11yEventRecord,
        sourceNode: A11yNodeSnapshot?,
    ): Boolean {
        val candidates = event.text + listOfNotNull(sourceNode?.text)
        return matchesText(candidates)
    }

    private fun matchesEventContentDescription(
        event: A11yEventRecord,
        sourceNode: A11yNodeSnapshot?,
    ): Boolean {
        val candidates = listOfNotNull(event.contentDescription, sourceNode?.contentDescription)
        return matchesContentDescription(candidates)
    }

    private fun matchesEventState(sourceNode: A11yNodeSnapshot?): Boolean {
        return if (requiresState() && sourceNode == null) {
            false
        } else {
            sourceNode == null || matchesState(sourceNode)
        }
    }

    private fun matchesText(value: String?): Boolean = matchesText(listOfNotNull(value))

    private fun matchesText(values: List<String>): Boolean {
        return (textEquals == null || values.any { it == textEquals }) &&
            (textContains == null || values.any { it.contains(textContains) })
    }

    private fun matchesContentDescription(value: String?): Boolean {
        return matchesContentDescription(listOfNotNull(value))
    }

    private fun matchesContentDescription(values: List<String>): Boolean {
        return (contentDescriptionEquals == null || values.any { it == contentDescriptionEquals }) &&
            (contentDescriptionContains == null || values.any { it.contains(contentDescriptionContains) })
    }

    private fun matchesState(node: A11yNodeSnapshot): Boolean {
        return (clickable == null || node.clickable == clickable) &&
            (editable == null || node.editable == editable) &&
            (enabled == null || node.enabled == enabled) &&
            (selected == null || node.selected == selected) &&
            (checked == null || node.checked == checked)
    }

    private fun requiresState(): Boolean {
        return clickable != null ||
            editable != null ||
            enabled != null ||
            selected != null ||
            checked != null
    }

    fun describe(): String {
        val parts = buildList {
            textEquals?.let { add("text == \"$it\"") }
            textContains?.let { add("text contains \"$it\"") }
            contentDescriptionEquals?.let { add("contentDescription == \"$it\"") }
            contentDescriptionContains?.let { add("contentDescription contains \"$it\"") }
            clickable?.let { add("clickable == $it") }
            editable?.let { add("editable == $it") }
            enabled?.let { add("enabled == $it") }
            selected?.let { add("selected == $it") }
            checked?.let { add("checked == $it") }
        }
        return if (parts.isEmpty()) "any" else parts.joinToString(", ")
    }
}
