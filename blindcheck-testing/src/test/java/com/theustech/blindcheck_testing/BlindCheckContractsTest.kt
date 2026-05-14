package com.theustech.blindcheck_testing

import com.theustech.blindcheck_testing.actions.UserAccessibilityActions
import com.theustech.blindcheck_testing.assertions.FeedbackExpectation
import com.theustech.blindcheck_testing.assertions.FocusExpectation
import com.theustech.blindcheck_testing.assertions.FocusSequenceExpectation
import com.theustech.blindcheck_testing.model.A11yEventRecord
import com.theustech.blindcheck_testing.model.A11yNodeSnapshot
import com.theustech.blindcheck_testing.model.BlindCheckFlow
import com.theustech.blindcheck_testing.model.RectSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlindCheckContractsTest {
    @Test
    fun userAccessibilityActions_exposesOnlyUserIntentActions() {
        val methodNames = UserAccessibilityActions::class.java.declaredMethods
            .map { it.name }
            .toSet()

        assertEquals(
            setOf(
                "next",
                "previous",
                "activate",
                "scrollForward",
                "scrollBackward",
                "inputText",
                "back",
            ),
            methodNames,
        )
    }

    @Test
    fun flow_usesVersionedSchemaAndNullableEndTimeByDefault() {
        val event = A11yEventRecord(
            id = "event-1",
            timestamp = 1770000000000,
            packageName = "com.example.app",
            eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
            text = listOf("Entrar"),
        )

        val flow = BlindCheckFlow(
            targetPackage = "com.example.app",
            startedAt = 1770000000000,
            events = listOf(event),
        )

        assertEquals("blindcheck-flow-v1", flow.schemaVersion)
        assertNull(flow.endedAt)
        assertEquals("com.example.app", flow.targetPackage)
        assertEquals(listOf(event), flow.events)
    }

    @Test
    fun nodeSnapshot_keepsSerializableAccessibilityState() {
        val child = A11yNodeSnapshot(text = "Child")
        val node = A11yNodeSnapshot(
            text = "E-mail",
            contentDescription = "Campo de e-mail",
            className = "android.widget.EditText",
            viewIdResourceName = "email",
            packageName = "com.example.app",
            clickable = true,
            enabled = true,
            focused = true,
            selected = true,
            checked = true,
            editable = true,
            actions = listOf("ACTION_SET_TEXT"),
            boundsInScreen = RectSnapshot(left = 1, top = 2, right = 3, bottom = 4),
            children = listOf(child),
        )

        assertEquals("E-mail", node.text)
        assertEquals("Campo de e-mail", node.contentDescription)
        assertEquals("android.widget.EditText", node.className)
        assertEquals("email", node.viewIdResourceName)
        assertEquals("com.example.app", node.packageName)
        assertTrue(node.clickable)
        assertTrue(node.enabled)
        assertTrue(node.focused)
        assertTrue(node.selected)
        assertTrue(node.checked)
        assertTrue(node.editable)
        assertEquals(listOf("ACTION_SET_TEXT"), node.actions)
        assertEquals(RectSnapshot(left = 1, top = 2, right = 3, bottom = 4), node.boundsInScreen)
        assertEquals(listOf(child), node.children)
    }

    @Test
    fun focusExpectation_matchesNodeTextContentDescriptionAndState() {
        val node = A11yNodeSnapshot(
            text = "Entrar",
            contentDescription = "Botao entrar",
            clickable = true,
            enabled = true,
        )

        assertTrue(
            FocusExpectation(
                textContains = "Entr",
                contentDescriptionContains = "entrar",
                clickable = true,
                enabled = true,
            ).matches(node),
        )

        assertFalse(FocusExpectation(textContains = "Senha").matches(node))
        assertFalse(FocusExpectation(clickable = false).matches(node))
    }

    @Test
    fun focusExpectation_matchesEventTextAndSourceNodeState() {
        val event = A11yEventRecord(
            id = "event-1",
            timestamp = 1,
            eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
            text = listOf("E-mail"),
            sourceNode = A11yNodeSnapshot(
                text = "Campo de e-mail",
                editable = true,
                enabled = true,
            ),
        )

        assertTrue(FocusExpectation(textContains = "E-mail", editable = true).matches(event))
        assertTrue(FocusExpectation(textContains = "Campo", enabled = true).matches(event))
        assertFalse(FocusExpectation(textContains = "Senha", editable = true).matches(event))
    }

    @Test
    fun feedbackExpectation_matchesEventTextOrContentDescription() {
        val textEvent = A11yEventRecord(
            id = "event-1",
            timestamp = 1,
            eventType = "TYPE_ANNOUNCEMENT",
            text = listOf("Informe o e-mail"),
        )
        val contentDescriptionEvent = A11yEventRecord(
            id = "event-2",
            timestamp = 2,
            eventType = "TYPE_VIEW_FOCUSED",
            contentDescription = "Informe a senha",
        )

        assertTrue(FeedbackExpectation(contains = "e-mail").matches(textEvent))
        assertTrue(FeedbackExpectation(contains = "senha").matches(contentDescriptionEvent))
        assertFalse(FeedbackExpectation(contains = "Frutas").matches(textEvent))
    }

    @Test
    fun focusSequenceExpectation_matchesOrderedSubsequenceThroughNoisyEvents() {
        val events = listOf(
            event("noise-1", "Decoracao"),
            event("title", "Acessar conta"),
            event("noise-2", "Separador"),
            event("email", "E-mail", editable = true),
            event("password", "Senha", editable = true),
            event("noise-3", "Rodape"),
            event("submit", "Entrar", clickable = true),
        )

        val expectation = FocusSequenceExpectation(
            items = listOf(
                FocusExpectation(textContains = "Acessar conta"),
                FocusExpectation(textContains = "E-mail", editable = true),
                FocusExpectation(textContains = "Senha", editable = true),
                FocusExpectation(textContains = "Entrar", clickable = true),
            ),
        )

        assertTrue(expectation.matches(events))
        assertFalse(
            FocusSequenceExpectation(
                items = listOf(
                    FocusExpectation(textContains = "Entrar"),
                    FocusExpectation(textContains = "E-mail"),
                ),
            ).matches(events),
        )
    }

    private fun event(
        id: String,
        text: String,
        clickable: Boolean = false,
        editable: Boolean = false,
    ): A11yEventRecord {
        return A11yEventRecord(
            id = id,
            timestamp = id.hashCode().toLong(),
            eventType = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
            text = listOf(text),
            sourceNode = A11yNodeSnapshot(
                text = text,
                clickable = clickable,
                editable = editable,
                enabled = true,
            ),
        )
    }
}
