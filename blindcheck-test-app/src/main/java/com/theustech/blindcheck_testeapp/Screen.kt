package com.theustech.blindcheck_testeapp

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Base contract for a Navigation Compose destination.
 *
 * Every screen renders through [Render], so accessibility-focus requests are gated by the
 * destination lifecycle and by the root container's first layout. A concrete screen can
 * register whichever native view represents its first accessible item; the base class has no
 * knowledge of titles, app bars, buttons, or the visual layout.
 */
abstract class Screen {

    @Composable
    final fun Render(backStackEntry: NavBackStackEntry) {
        val isResumed = rememberIsDestinationShowing(backStackEntry)
        var hasRootLayout by remember(backStackEntry.id) { mutableStateOf(false) }
        var initialAccessibilityTarget by remember(backStackEntry.id) { mutableStateOf<View?>(null) }
        var hasRequestedAccessibilityFocus by remember(backStackEntry.id) { mutableStateOf(false) }

        Content(
            modifier = Modifier.onGloballyPositioned {
                hasRootLayout = true
            },
            registerInitialAccessibilityTarget = { target ->
                initialAccessibilityTarget = target
            },
        )

        LaunchedEffect(
            backStackEntry.id,
            isResumed,
            hasRootLayout,
            initialAccessibilityTarget,
        ) {
            val target = initialAccessibilityTarget
            if (
                shouldRequestInitialAccessibilityFocus(
                    isDestinationResumed = isResumed,
                    hasRootLayout = hasRootLayout,
                    hasRegisteredTarget = target != null,
                    hasAlreadyRequestedFocus = hasRequestedAccessibilityFocus,
                )
            ) {
                target?.awaitAttachmentAndLayout()
                // Wait for the next Compose frame so its semantics provider can publish the
                // native target without introducing an arbitrary time-based delay.
                withFrameNanos { }
                if (
                    backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    target?.isAttachedToWindow == true &&
                    target.isLaidOut
                ) {
                    target.performAccessibilityAction(
                        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                        null,
                    )
                    hasRequestedAccessibilityFocus = true
                }
            }
        }
    }

    @Composable
    protected abstract fun Content(
        modifier: Modifier,
        registerInitialAccessibilityTarget: (View) -> Unit,
    )
}

/**
 * Whether this destination is the one currently being shown.
 *
 * `RESUMED` is the boundary Navigation Compose uses: while a transition runs, the destination
 * being left drops to `STARTED` and stays composed alongside the one arriving. That edge is what
 * both accessibility strategies key off — one to avoid pulling focus into content on its way out,
 * the other to retire that content from the accessibility tree while it is still on screen.
 */
@Composable
internal fun rememberIsDestinationShowing(backStackEntry: NavBackStackEntry): Boolean {
    var isShowing by remember(backStackEntry.id) {
        mutableStateOf(backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(backStackEntry) {
        val observer = LifecycleEventObserver { _, _ ->
            isShowing = backStackEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        backStackEntry.lifecycle.addObserver(observer)
        onDispose {
            backStackEntry.lifecycle.removeObserver(observer)
        }
    }

    return isShowing
}

internal fun shouldRequestInitialAccessibilityFocus(
    isDestinationResumed: Boolean,
    hasRootLayout: Boolean,
    hasRegisteredTarget: Boolean,
    hasAlreadyRequestedFocus: Boolean,
): Boolean =
    isDestinationResumed &&
        hasRootLayout &&
        hasRegisteredTarget &&
        !hasAlreadyRequestedFocus

private suspend fun View.awaitAttachmentAndLayout() {
    if (isAttachedToWindow && isLaidOut) return

    suspendCancellableCoroutine { continuation ->
        lateinit var attachListener: View.OnAttachStateChangeListener
        lateinit var layoutListener: View.OnLayoutChangeListener

        fun removeListeners() {
            removeOnAttachStateChangeListener(attachListener)
            removeOnLayoutChangeListener(layoutListener)
        }

        fun resumeWhenReady() {
            if (isAttachedToWindow && isLaidOut && continuation.isActive) {
                removeListeners()
                continuation.resume(Unit)
            }
        }

        attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = resumeWhenReady()

            override fun onViewDetachedFromWindow(view: View) = Unit
        }
        layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            resumeWhenReady()
        }

        addOnAttachStateChangeListener(attachListener)
        addOnLayoutChangeListener(layoutListener)
        continuation.invokeOnCancellation { removeListeners() }
        resumeWhenReady()
    }
}
