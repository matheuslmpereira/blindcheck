package com.theustech.blindcheck_focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialAccessibilityFocusTest {

    private fun candidate(
        nodeId: Int,
        top: Float = 0f,
        left: Float = 0f,
        width: Float = 100f,
        height: Float = 40f,
        traversalIndex: Float = 0f,
        isHiddenFromAccessibility: Boolean = false,
        describesItself: Boolean = true,
        isActionable: Boolean = false,
    ) = AccessibilityFocusCandidate(
        nodeId = nodeId,
        top = top,
        left = left,
        width = width,
        height = height,
        traversalIndex = traversalIndex,
        isHiddenFromAccessibility = isHiddenFromAccessibility,
        describesItself = describesItself,
        isActionable = isActionable,
    )

    @Test
    fun `selects the topmost focusable node`() {
        val selected = selectInitialAccessibilityFocus(
            listOf(
                candidate(nodeId = 2, top = 200f),
                candidate(nodeId = 1, top = 50f),
                candidate(nodeId = 3, top = 400f),
            ),
        )

        assertEquals(1, selected)
    }

    @Test
    fun `breaks ties on the same row from left to right`() {
        val selected = selectInitialAccessibilityFocus(
            listOf(
                candidate(nodeId = 7, top = 50f, left = 300f),
                candidate(nodeId = 8, top = 50f, left = 20f),
            ),
        )

        assertEquals(8, selected)
    }

    @Test
    fun `honours traversalIndex before geometry`() {
        val selected = selectInitialAccessibilityFocus(
            listOf(
                candidate(nodeId = 1, top = 0f),
                candidate(nodeId = 2, top = 900f, traversalIndex = -1f),
            ),
        )

        assertEquals(2, selected)
    }

    @Test
    fun `ignores nodes hidden from accessibility`() {
        val selected = selectInitialAccessibilityFocus(
            listOf(
                candidate(nodeId = 1, top = 0f, isHiddenFromAccessibility = true),
                candidate(nodeId = 2, top = 100f),
            ),
        )

        assertEquals(2, selected)
    }

    @Test
    fun `ignores zero sized nodes`() {
        val selected = selectInitialAccessibilityFocus(
            listOf(
                candidate(nodeId = 1, top = 0f, width = 0f, height = 0f),
                candidate(nodeId = 2, top = 100f),
            ),
        )

        assertEquals(2, selected)
    }

    @Test
    fun `ignores nodes that neither describe themselves nor expose actions`() {
        val selected = selectInitialAccessibilityFocus(
            listOf(
                candidate(nodeId = 1, top = 0f, describesItself = false),
                candidate(nodeId = 2, top = 100f, describesItself = false, isActionable = true),
            ),
        )

        assertEquals(2, selected)
    }

    @Test
    fun `returns null while the subtree has nothing focusable`() {
        assertNull(selectInitialAccessibilityFocus(emptyList()))
        assertNull(
            selectInitialAccessibilityFocus(
                listOf(candidate(nodeId = 1, describesItself = false)),
            ),
        )
    }

    @Test
    fun `is deterministic for perfectly overlapping nodes`() {
        val overlapping = listOf(candidate(nodeId = 9), candidate(nodeId = 4))

        assertEquals(4, selectInitialAccessibilityFocus(overlapping))
        assertEquals(4, selectInitialAccessibilityFocus(overlapping.reversed()))
    }

    @Test
    fun `an actionable node without description can hold focus`() {
        assertTrue(candidate(nodeId = 1, describesItself = false, isActionable = true).canHoldScreenReaderFocus())
        assertFalse(candidate(nodeId = 1, describesItself = false, isActionable = false).canHoldScreenReaderFocus())
    }
}
