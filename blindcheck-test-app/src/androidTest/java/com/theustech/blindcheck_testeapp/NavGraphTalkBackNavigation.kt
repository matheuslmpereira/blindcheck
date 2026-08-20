package com.theustech.blindcheck_testeapp

import android.app.UiAutomation
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry

/** Launches the mockup app directly on one NavGraph accessibility approach. */
internal fun launchNavGraphApproach(
    approach: NavGraphAccessibilityApproach,
): ActivityScenario<MainActivity> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(EXTRA_NAVGRAPH_ACCESSIBILITY_APPROACH, approach.argumentValue)
    }
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    // Blocks until the new activity is resumed, so the wait below cannot be satisfied by the
    // window a previous launch left on screen.
    scenario.onActivity { }
    awaitMockupWindow()
    return scenario
}

/**
 * Waits for the mockup app window to be published to the accessibility layer.
 *
 * A cold start of this app takes seconds on an emulator running TalkBack, and asserting before the
 * window exists reports an empty tree instead of the real content.
 */
private fun awaitMockupWindow() {
    val automation = InstrumentationRegistry.getInstrumentation()
        .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    val deadline = SystemClock.uptimeMillis() + WINDOW_TIMEOUT_MS
    do {
        automation.waitForIdle(IDLE_QUIET_MS, IDLE_TIMEOUT_MS)
        // Only the active window counts: a window left behind by a previous launch can still
        // expose labels while the new activity is starting.
        val active = automation.rootInActiveWindow
        if (active?.packageName == MOCKUP_PACKAGE && active.collectLabels().isNotEmpty()) {
            // Require the same content twice: during a destination swap the window briefly
            // publishes an empty tree.
            SystemClock.sleep(WINDOW_POLL_MS)
            val stable = automation.rootInActiveWindow
            if (stable?.packageName == MOCKUP_PACKAGE && stable.collectLabels().isNotEmpty()) return
        }
        SystemClock.sleep(WINDOW_POLL_MS)
    } while (SystemClock.uptimeMillis() < deadline)
    throw AssertionError("The mockup app window did not appear within ${WINDOW_TIMEOUT_MS}ms.")
}

/**
 * Focuses the node labelled [label] and activates it the way TalkBack does on a double tap:
 * `ACTION_CLICK` on the node holding accessibility focus.
 *
 * Injected taps are not usable here — with TalkBack bound they are consumed by touch exploration —
 * and the speech-level fidelity of the transition is covered by the controlled TTS capture.
 */
internal fun activateWithAccessibilityFocus(label: String) {
    val automation = InstrumentationRegistry.getInstrumentation()
        .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    var lastFailure = "no attempt was made"
    repeat(ACTIVATION_ATTEMPTS) {
        // The tree is re-read on every attempt: a node captured from the previous destination is
        // stale and silently refuses actions.
        automation.waitForIdle(IDLE_QUIET_MS, IDLE_TIMEOUT_MS)
        val roots = buildList {
            automation.rootInActiveWindow?.let(::add)
            automation.windows.mapNotNull { it.root }.forEach(::add)
        }
        val target = roots.firstNotNullOfOrNull { root -> root.findByLabel(label) }
        if (target == null) {
            lastFailure = "no node labelled \"$label\"; observed " +
                roots.flatMap { it.collectLabels() }.distinct()
            return@repeat
        }
        val clickable = generateSequence(target) { it.parent }.firstOrNull { it.isClickable }
        if (clickable == null || !clickable.refresh()) {
            lastFailure = "node labelled \"$label\" is stale or exposes no clickable node"
            return@repeat
        }

        // Move accessibility focus first and then activate, exactly like a TalkBack double tap.
        clickable.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
        if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
        lastFailure = "node labelled \"$label\" did not accept ACTION_CLICK"
    }
    throw AssertionError("Could not activate \"$label\" after $ACTIVATION_ATTEMPTS attempts: $lastFailure.")
}

private const val MOCKUP_PACKAGE = "com.theustech.blindcheck_testeapp"
private const val WINDOW_TIMEOUT_MS = 15_000L
private const val WINDOW_POLL_MS = 250L
private const val ACTIVATION_ATTEMPTS = 5
private const val IDLE_QUIET_MS = 500L
private const val IDLE_TIMEOUT_MS = 5_000L

private fun AccessibilityNodeInfo.findByLabel(label: String): AccessibilityNodeInfo? {
    if (text?.toString() == label || contentDescription?.toString() == label) return this
    for (index in 0 until childCount) {
        getChild(index)?.findByLabel(label)?.let { return it }
    }
    return null
}

private fun AccessibilityNodeInfo.collectLabels(): List<String> = buildList {
    (text ?: contentDescription)?.toString()?.takeIf { it.isNotBlank() }?.let(::add)
    for (index in 0 until childCount) {
        addAll(getChild(index)?.collectLabels().orEmpty())
    }
}
