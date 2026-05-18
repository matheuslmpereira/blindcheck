package com.theustech.blindcheck_interactor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.theustech.blindcheck_tracker.A11yEventNormalizer
import com.theustech.blindcheck_tracker.RemoteActions
import com.theustech.blindcheck_tracker.TrackingEventStore

class TrackingAccessibilityService : AccessibilityService(), ActionExecutor {

    private val normalizer = A11yEventNormalizer()
    private val eventStore = TrackingEventStore.shared

    override fun onServiceConnected() {
        instance = this
        executor = this
    }

    override fun onDestroy() {
        instance = null
        executor = null
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        eventStore.record(normalizer.normalize(event))
    }

    override fun onInterrupt() = Unit

    override fun execute(action: String) {
        Log.d(TAG, "Executing remote action: $action")
        when (action) {
            RemoteActions.ACTION_NEXT           -> swipeHorizontal(forward = true)
            RemoteActions.ACTION_PREVIOUS       -> swipeHorizontal(forward = false)
            RemoteActions.ACTION_ACTIVATE       -> doubleTap()
            RemoteActions.ACTION_BACK           -> performGlobalAction(GLOBAL_ACTION_BACK)
            RemoteActions.ACTION_SCROLL_FORWARD -> twoFingerSwipeVertical(up = true)
            RemoteActions.ACTION_SCROLL_BACKWARD-> twoFingerSwipeVertical(up = false)
            RemoteActions.ACTION_HOME           -> performGlobalAction(GLOBAL_ACTION_HOME)
            RemoteActions.ACTION_RECENTS        -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            RemoteActions.ACTION_SWIPE_UP       -> swipeVertical(up = true)
            RemoteActions.ACTION_SWIPE_DOWN     -> swipeVertical(up = false)
        }
    }

    // TalkBack interprets a right swipe as "next element", left swipe as "previous".
    private fun swipeHorizontal(forward: Boolean) {
        val w = resources.displayMetrics.widthPixels
        val cy = resources.displayMetrics.heightPixels / 2f
        val from = if (forward) w * 0.25f else w * 0.75f
        val to   = if (forward) w * 0.75f else w * 0.25f
        gesture(Path().apply { moveTo(from, cy); lineTo(to, cy) }, durationMs = 150)
    }

    // TalkBack activates the focused element on double tap anywhere on screen.
    private fun doubleTap() {
        val cx = resources.displayMetrics.widthPixels / 2f
        val cy = resources.displayMetrics.heightPixels / 2f
        val path = Path().apply { moveTo(cx, cy) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 1L))
            .addStroke(GestureDescription.StrokeDescription(path, 100L, 1L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // Two simultaneous finger strokes — TalkBack passes these through as scroll events.
    private fun twoFingerSwipeVertical(up: Boolean) {
        val cx = resources.displayMetrics.widthPixels / 2f
        val h  = resources.displayMetrics.heightPixels
        val from = if (up) h * 0.7f else h * 0.3f
        val to   = if (up) h * 0.3f else h * 0.7f
        val path1 = Path().apply { moveTo(cx - 50, from); lineTo(cx - 50, to) }
        val path2 = Path().apply { moveTo(cx + 50, from); lineTo(cx + 50, to) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0L, 400L))
            .addStroke(GestureDescription.StrokeDescription(path2, 0L, 400L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // Single-finger vertical swipe (app drawer, notification shade, etc.).
    private fun swipeVertical(up: Boolean) {
        val cx = resources.displayMetrics.widthPixels / 2f
        val h  = resources.displayMetrics.heightPixels
        val from = if (up) h * 0.8f else h * 0.2f
        val to   = if (up) h * 0.2f else h * 0.8f
        gesture(Path().apply { moveTo(cx, from); lineTo(cx, to) }, durationMs = 600)
    }

    private fun gesture(path: Path, durationMs: Long) {
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
            .build()
        dispatchGesture(gesture, null, null)
    }

    companion object {
        private const val TAG = "BlindCheckTracker"

        @Volatile
        var instance: TrackingAccessibilityService? = null
            private set

        @Volatile
        internal var executor: ActionExecutor? = null

        fun setExecutorForTest(e: ActionExecutor?) {
            executor = e
        }
    }
}
