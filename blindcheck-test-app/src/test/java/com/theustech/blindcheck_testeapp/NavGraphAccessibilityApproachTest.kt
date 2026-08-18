package com.theustech.blindcheck_testeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavGraphAccessibilityApproachTest {

    @Test
    fun `experiments change exactly one accessibility variable from baseline`() {
        val strategies = NavGraphAccessibilityApproach.experiments.associateBy { it.argumentValue }

        assertEquals(8, strategies.size)
        assertTrue(strategies.getValue("unique-labels").usesUniqueLabels)
        assertTrue(strategies.getValue("unique-node-ids").exposesUniqueNodeIds)
        assertTrue(strategies.getValue("recreated-semantics").recreatesDestinationSemantics)
        assertTrue(strategies.getValue("pane-title").announcesPaneTitle)
        assertTrue(strategies.getValue("imperative-focus").requestsImperativeAccessibilityFocus)
        assertTrue(strategies.getValue("agnostic-focus-reset").usesLibraryFocusReset)
        assertTrue(strategies.getValue("unique-labels-pane-title").usesUniqueLabels)
        assertTrue(strategies.getValue("unique-labels-pane-title").announcesPaneTitle)

        strategies
            .filterValues { it.isolatesSingleVariable }
            .filterKeys { it != "baseline" }
            .forEach { (name, strategy) ->
            val enabledVariables = listOf(
                strategy.usesUniqueLabels,
                strategy.exposesUniqueNodeIds,
                strategy.recreatesDestinationSemantics,
                strategy.announcesPaneTitle,
                strategy.requestsImperativeAccessibilityFocus,
                strategy.usesLibraryFocusReset,
            ).count { it }
            assertEquals("$name must isolate one variable", 1, enabledVariables)
            }
    }

    @Test
    fun `unique label approach gives each destination a distinct accessible name`() {
        val approach = NavGraphAccessibilityApproach.UniqueLabels

        assertEquals("continuar 1", approach.continueLabel(1))
        assertEquals("continuar 2", approach.continueLabel(2))
        assertEquals("continuar 3", approach.continueLabel(3))
    }

    @Test
    fun `unique id approach exposes a distinct stable id without changing the label`() {
        val approach = NavGraphAccessibilityApproach.UniqueNodeIds

        assertEquals("Continuar", approach.continueLabel(1))
        assertEquals("navgraph_continue_page_1", approach.continueNodeId(1))
        assertEquals("navgraph_continue_page_2", approach.continueNodeId(2))
        assertEquals("navgraph_continue_page_3", approach.continueNodeId(3))
        assertNull(NavGraphAccessibilityApproach.Baseline.continueNodeId(1))
    }

    @Test
    fun `agnostic focus reset keeps the ambiguous labels and adds no per screen infrastructure`() {
        val agnostic = NavGraphAccessibilityApproach.AgnosticFocusReset
        val baseline = NavGraphAccessibilityApproach.Baseline

        assertTrue(agnostic.usesLibraryFocusReset)
        assertFalse(agnostic.requestsImperativeAccessibilityFocus)
        assertFalse(agnostic.usesUniqueLabels)
        assertFalse(agnostic.announcesPaneTitle)
        assertTrue(agnostic.isExperiment)
        (1..3).forEach { page ->
            assertEquals(baseline.continueLabel(page), agnostic.continueLabel(page))
        }
        assertNull(agnostic.continueNodeId(1))
    }

    @Test
    fun `legacy combined reset is preserved but excluded from isolated experiments`() {
        val legacy = NavGraphAccessibilityApproach.LegacyCombinedReset

        assertFalse(legacy.isExperiment)
        assertTrue(legacy.recreatesDestinationSemantics)
        assertTrue(legacy.announcesPaneTitle)
        assertTrue(legacy.requestsImperativeAccessibilityFocus)
        assertFalse(NavGraphAccessibilityApproach.experiments.contains(legacy))
    }
}
