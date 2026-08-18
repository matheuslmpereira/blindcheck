package com.theustech.blindcheck_focus

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import java.util.concurrent.atomic.AtomicLong

/**
 * Marks the subtree that owns an accessibility-focus reset. The value is an internal identifier,
 * never app content, and the parent policy keeps the outermost reset root when two are nested.
 */
private val AccessibilityFocusResetRoot = SemanticsPropertyKey<String>(
    name = "BlindCheckAccessibilityFocusResetRoot",
    mergePolicy = { parentValue, _ -> parentValue },
)

private val resetRootIds = AtomicLong(0L)

/**
 * Number of frames the reset waits for the subtree to publish a focusable node before giving up.
 * There is no fixed delay: each attempt happens on a real Compose frame.
 */
private const val MAX_FRAME_ATTEMPTS = 30

/**
 * Moves screen-reader focus to the first accessible item of this subtree once, when [key] changes.
 *
 * Android keeps accessibility focus on the node it considers equivalent after a window content
 * change. When two destinations expose the same label — the classic `Continuar` on every step of a
 * flow — the person hears the same announcement again and nothing tells them the screen changed.
 * Applying this modifier to a destination root restarts focus at the beginning of the new content.
 *
 * The modifier is content-agnostic: it does not know about titles, app bars, or buttons, and the
 * host screen registers nothing. The target is chosen from the semantics tree of this subtree only,
 * so surrounding chrome and any destination still leaving the composition are never selected.
 *
 * @param key identity of the content being shown. In Navigation Compose, pass the destination's
 * `NavBackStackEntry.id`; the reset re-arms whenever this value changes and runs at most once per
 * value, so it never pulls focus back while the person navigates inside the destination.
 * @param enabled set to `false` to keep the platform default behaviour.
 */
fun Modifier.resetAccessibilityFocusOnEnter(
    key: Any?,
    enabled: Boolean = true,
): Modifier = composed {
    val view = LocalView.current
    val rootId = remember(key) { "blindcheck-focus-reset-${resetRootIds.incrementAndGet()}" }
    var hasSubtreeLayout by remember(key) { mutableStateOf(false) }
    var hasAlreadyReset by rememberSaveable(key) { mutableStateOf(false) }

    LaunchedEffect(key, view, enabled, hasSubtreeLayout) {
        if (
            !shouldResetAccessibilityFocus(
                isEnabled = enabled,
                isAccessibilityEnabled = view.context.isAccessibilityEnabled(),
                hasSubtreeLayout = hasSubtreeLayout,
                hasAlreadyReset = hasAlreadyReset,
            )
        ) {
            return@LaunchedEffect
        }

        repeat(MAX_FRAME_ATTEMPTS) {
            // Wait for a real frame instead of an arbitrary delay: the semantics owner only
            // publishes the new subtree after Compose applies and lays out the destination.
            withFrameNanos { }
            val targetNodeId = view.findInitialAccessibilityFocusTarget(rootId)
            if (targetNodeId != null && view.requestAccessibilityFocus(targetNodeId)) {
                hasAlreadyReset = true
                return@LaunchedEffect
            }
        }
    }

    this
        .semantics { this[AccessibilityFocusResetRoot] = rootId }
        .onGloballyPositioned { hasSubtreeLayout = true }
}

private fun Context.isAccessibilityEnabled(): Boolean {
    val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    return manager?.isEnabled == true
}

/**
 * Resolves the first focusable node of the subtree tagged with [rootId], or `null` while the
 * subtree is not published yet. A `null` [rootId] resolves the first reset root in the tree, which
 * is how tests inspect the resolution without reaching into the generated identifier.
 */
internal fun View.findInitialAccessibilityFocusTarget(rootId: String? = null): Int? {
    val owner = semanticsOwnerOrNull() ?: return null
    val unmergedNodes = owner.getAllSemanticsNodes(mergingEnabled = false)
    val resetRoot = unmergedNodes.firstOrNull { node ->
        val nodeRootId = node.config.getOrNull(AccessibilityFocusResetRoot)
        nodeRootId != null && (rootId == null || nodeRootId == rootId)
    } ?: return null

    val subtreeNodeIds = resetRoot.collectSubtreeNodeIds()
    val candidates = owner.getAllSemanticsNodes(mergingEnabled = true)
        .filter { it.id != resetRoot.id && it.id in subtreeNodeIds }
        .map { it.toFocusCandidate() }

    return selectInitialAccessibilityFocus(candidates)
}

internal fun View.requestAccessibilityFocus(nodeId: Int): Boolean =
    accessibilityNodeProvider?.performAction(
        nodeId,
        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
        null,
    ) == true

private fun SemanticsNode.collectSubtreeNodeIds(): Set<Int> {
    val ids = mutableSetOf<Int>()
    val pending = ArrayDeque(listOf(this))
    while (pending.isNotEmpty()) {
        val node = pending.removeFirst()
        if (!ids.add(node.id)) continue
        pending.addAll(node.children)
    }
    return ids
}

private fun SemanticsNode.toFocusCandidate(): AccessibilityFocusCandidate {
    val bounds = boundsInWindow
    return AccessibilityFocusCandidate(
        nodeId = id,
        top = bounds.top,
        left = bounds.left,
        width = bounds.width,
        height = bounds.height,
        traversalIndex = config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f,
        isHiddenFromAccessibility = isHiddenFromAccessibility(),
        describesItself = describesItself(),
        isActionable = isActionable(),
    )
}

private fun SemanticsNode.describesItself(): Boolean {
    val hasText = config.getOrNull(SemanticsProperties.Text)?.any { it.text.isNotBlank() } == true
    val hasContentDescription = config.getOrNull(SemanticsProperties.ContentDescription)
        ?.any { it.isNotBlank() } == true
    val hasEditableText = config.getOrNull(SemanticsProperties.EditableText)?.text?.isNotBlank() == true
    val hasStateDescription = config.getOrNull(SemanticsProperties.StateDescription)?.isNotBlank() == true
    return hasText || hasContentDescription || hasEditableText || hasStateDescription
}

private fun SemanticsNode.isActionable(): Boolean =
    config.getOrNull(SemanticsActions.OnClick) != null ||
        config.getOrNull(SemanticsActions.OnLongClick) != null ||
        config.getOrNull(SemanticsActions.SetText) != null

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
private fun SemanticsNode.isHiddenFromAccessibility(): Boolean =
    config.getOrNull(SemanticsProperties.InvisibleToUser) != null

private fun View.semanticsOwnerOrNull(): SemanticsOwner? = (this as? RootForTest)?.semanticsOwner
