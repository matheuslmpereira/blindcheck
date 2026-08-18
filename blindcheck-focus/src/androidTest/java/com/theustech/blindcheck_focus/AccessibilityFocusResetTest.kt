package com.theustech.blindcheck_focus

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration coverage for the resolution step: the modifier must find the first accessible item of
 * its own subtree from the live semantics tree, for arbitrary content it knows nothing about.
 *
 * The hand-off to a screen reader itself is covered by the instrumented TalkBack test and by the
 * controlled TTS capture, because `UiAutomation` does not reliably expose the focus TalkBack holds.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityFocusResetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun resolvesTheFirstAccessibleItemOfTheSubtree() {
        assertEquals(
            "primeiro item",
            resolveInitialFocusText {
                Text("primeiro item")
                Button(onClick = {}) { Text("Continuar") }
            },
        )
    }

    @Test
    fun ignoresDecorationWithoutAccessibleContent() {
        assertEquals(
            "conteúdo real",
            resolveInitialFocusText {
                Box(modifier = Modifier.size(48.dp))
                Text("conteúdo real")
            },
        )
    }

    @Test
    fun honoursTraversalIndexDeclaredByTheScreen() {
        assertEquals(
            "promovido pela tela",
            resolveInitialFocusText {
                Text("visualmente primeiro")
                Text(
                    text = "promovido pela tela",
                    modifier = Modifier.semantics { traversalIndex = -1f },
                )
            },
        )
    }

    @Test
    fun neverSelectsContentOutsideTheResetSubtree() {
        assertEquals(
            "primeiro item do destino",
            resolveInitialFocusText(
                outsideContent = { Text("cabeçalho fora do destino") },
            ) {
                Text("primeiro item do destino")
            },
        )
    }

    @Test
    fun selectsAnActionableNodeWhenTheScreenStartsWithOne() {
        assertEquals(
            "Continuar",
            resolveInitialFocusText {
                Button(onClick = {}) { Text("Continuar") }
                Text("texto abaixo")
            },
        )
    }

    @Test
    fun resolvesTheNewSubtreeAfterTheKeyChanges() {
        lateinit var hostView: View
        var page by mutableStateOf(1)

        composeRule.setContent {
            hostView = LocalView.current
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .resetAccessibilityFocusOnEnter(key = page),
            ) {
                Text("Tela $page")
                Button(onClick = { page += 1 }) { Text("Continuar") }
            }
        }
        composeRule.waitForIdle()
        assertEquals("Tela 1", hostView.initialFocusText())

        composeRule.onNodeWithText("Continuar").performClick()
        composeRule.waitForIdle()

        assertEquals("Tela 2", hostView.initialFocusText())
    }

    private fun resolveInitialFocusText(
        outsideContent: @Composable () -> Unit = {},
        destinationContent: @Composable () -> Unit,
    ): String? {
        lateinit var hostView: View
        composeRule.setContent {
            hostView = LocalView.current
            Column(modifier = Modifier.fillMaxSize()) {
                outsideContent()
                Column(
                    modifier = Modifier.resetAccessibilityFocusOnEnter(key = "destination"),
                ) {
                    destinationContent()
                }
            }
        }
        composeRule.waitForIdle()
        return hostView.initialFocusText()
    }

    private fun View.initialFocusText(): String? {
        val targetId = findInitialAccessibilityFocusTarget() ?: return null
        val owner = (this as RootForTest).semanticsOwner
        val node = owner.getAllSemanticsNodes(mergingEnabled = true)
            .firstOrNull { it.id == targetId }
            ?: return null
        return node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
            ?: node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
    }
}
