package com.theustech.blindcheck_focus

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
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

/** Names Compose has used for "this node is hidden from accessibility", oldest first. */
private val HIDDEN_FROM_ACCESSIBILITY_PROPERTIES = setOf("InvisibleToUser", "HideFromAccessibility")

/**
 * Restarts screen-reader focus for this subtree once, when [key] changes.
 *
 * Android keeps accessibility focus on the node it considers equivalent after a window content
 * change. When two destinations expose the same label — the classic `Continuar` on every step of a
 * flow — the person hears the same announcement again and nothing tells them the screen changed.
 * Applying this modifier to a destination root breaks that continuity.
 *
 * The modifier is content-agnostic: it does not know about titles, app bars, or buttons, and the
 * host screen registers nothing. Everything it touches comes from the semantics tree of this
 * subtree only, so surrounding chrome and any destination still leaving the composition are never
 * involved.
 *
 * @param key identity of the content being shown. In Navigation Compose, pass the destination's
 * `NavBackStackEntry.id`; the reset re-arms whenever this value changes and runs at most once per
 * value, so it never pulls focus back while the person navigates inside the destination.
 * @param enabled set to `false` to keep the platform default behaviour.
 * @param strategy which of the two measured approaches to apply. See
 * [AccessibilityFocusResetStrategy].
 * @param isShowing whether this content is the one currently being shown. Pass `false` while the
 * destination is leaving — in Navigation Compose, its back stack entry falling below `RESUMED`.
 * [AccessibilityFocusResetStrategy.RetireLeavingContent] acts precisely on that edge, and
 * [AccessibilityFocusResetStrategy.MoveFocusToFirstItem] uses it to avoid pulling focus into
 * content that is on its way out.
 */
@Composable
fun Modifier.resetAccessibilityFocusOnEnter(
    key: Any?,
    enabled: Boolean = true,
    strategy: AccessibilityFocusResetStrategy = AccessibilityFocusResetStrategy.MoveFocusToFirstItem,
    isShowing: Boolean = true,
): Modifier {
    val view = LocalView.current
    val rootId = remember(key) { "blindcheck-focus-reset-${resetRootIds.incrementAndGet()}" }
    var hasSubtreeLayout by remember(key) { mutableStateOf(false) }
    var hasAlreadyReset by rememberSaveable(key) { mutableStateOf(false) }
    var hasRetired by remember(key) { mutableStateOf(false) }

    LaunchedEffect(key, view, enabled, strategy, isShowing, hasSubtreeLayout) {
        val isAccessibilityEnabled = view.context.isAccessibilityEnabled()

        when (strategy) {
            AccessibilityFocusResetStrategy.MoveFocusToFirstItem -> {
                if (
                    !shouldResetAccessibilityFocus(
                        isEnabled = enabled,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isContentShowing = isShowing,
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

            AccessibilityFocusResetStrategy.RetireLeavingContent -> {
                if (
                    !shouldRetireLeavingContent(
                        isEnabled = enabled,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isContentShowing = isShowing,
                        hasAlreadyRetired = hasRetired,
                    )
                ) {
                    return@LaunchedEffect
                }

                // Order matters and is the whole point of this strategy: the focus has to be
                // released while the nodes that hold it still exist. Dropping the subtree first
                // would leave the reader holding a node that no longer answers.
                view.clearAccessibilityFocusInside(rootId)
                hasRetired = true
            }
        }
    }

    if (hasRetired) {
        // The subtree is published as one node with no properties: no text, no description, no
        // action, so nothing in it can hold screen-reader focus any more.
        //
        // `clearAndSetSemantics` rather than a "hide from accessibility" property because this
        // module targets a Compose version where that property exists only as the experimental
        // `invisibleToUser`, since renamed to `HideFromAccessibility`. Clearing is stable API and
        // has the same observable effect for a reader.
        return this.clearAndSetSemantics { }
    }

    return this
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
    val resetRoot = owner.resetRootOrNull(rootId) ?: return null

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

/**
 * Releases screen-reader focus held anywhere inside the subtree tagged with [rootId], and reports
 * whether any node was actually holding it.
 *
 * Every node of the subtree is asked to clear rather than the one node known to be focused, because
 * there is no public way to ask which virtual node currently holds accessibility focus. The action
 * is a no-op on a node that is not focused — Compose only clears when the id matches the focused
 * one — so asking all of them costs one traversal and reads nothing about the content.
 */
internal fun View.clearAccessibilityFocusInside(rootId: String? = null): Boolean {
    val provider = accessibilityNodeProvider ?: return false
    val owner = semanticsOwnerOrNull() ?: return false
    val resetRoot = owner.resetRootOrNull(rootId) ?: return false

    return resetRoot.collectSubtreeNodeIds().count { nodeId ->
        provider.performAction(
            nodeId,
            AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS,
            null,
        )
    } > 0
}

private fun SemanticsOwner.resetRootOrNull(rootId: String?): SemanticsNode? =
    getAllSemanticsNodes(mergingEnabled = false).firstOrNull { node ->
        val nodeRootId = node.config.getOrNull(AccessibilityFocusResetRoot)
        nodeRootId != null && (rootId == null || nodeRootId == rootId)
    }

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

/**
 * Whether the screen marked this node as hidden from accessibility.
 *
 * The property is matched by name instead of by symbol on purpose. Compose renamed
 * `InvisibleToUser` to `HideFromAccessibility` and deprecated the former, and the older symbol also
 * required an experimental opt-in. Reading the configuration keeps this working across both names
 * without depending on a deprecated or experimental API.
 */
private fun SemanticsNode.isHiddenFromAccessibility(): Boolean =
    config.any { (property, _) -> property.name in HIDDEN_FROM_ACCESSIBILITY_PROPERTIES }

/**
 * The only place where this library depends on the host view exposing the semantics tree.
 *
 * `RootForTest` is stable public API of `compose-ui`, but it is the single coupling point worth
 * watching on a Compose upgrade. The cast is deliberately safe: if a future version stops exposing
 * the owner here, the reset simply does not run and the screen keeps the platform default
 * behaviour — no crash and no behaviour change beyond losing the reset. The instrumented tests
 * assert the resolution, so an upgrade that breaks it fails the build instead of shipping.
 */
private fun View.semanticsOwnerOrNull(): SemanticsOwner? = (this as? RootForTest)?.semanticsOwner
