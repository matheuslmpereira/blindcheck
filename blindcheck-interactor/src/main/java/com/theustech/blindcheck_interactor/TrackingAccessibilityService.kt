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
        val record = normalizer.normalize(event)
        eventStore.record(record)
        logAnnouncement(event)
    }

    private fun logAnnouncement(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                val msg = buildFocusMessage(event) ?: return
                Log.i(ANNOUNCE_TAG, "FOCUS $msg")
            }
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                val msg = event.text.joinToString(" ").takeIf { it.isNotBlank() } ?: return
                Log.i(ANNOUNCE_TAG, "ANN $msg")
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val title = event.text.joinToString(" ").ifBlank {
                    event.contentDescription?.toString()
                } ?: return
                Log.i(ANNOUNCE_TAG, "WIN $title")
            }
        }
    }

    private fun buildFocusMessage(event: AccessibilityEvent): String? {
        val node = event.source ?: return null
        return try {
            val text = node.text?.toString()?.trim()
            val cd = node.contentDescription?.toString()?.trim()
            val content = when {
                !text.isNullOrBlank() && !cd.isNullOrBlank() -> "$text, $cd"
                !text.isNullOrBlank() -> text
                !cd.isNullOrBlank() -> cd
                event.text.isNotEmpty() -> event.text.joinToString(", ")
                else -> return null
            }
            val role = roleFromClassName(node.className?.toString())
            val states = buildList {
                if (!node.isEnabled) add("desabilitado")
                if (node.isEditable) add("editável")
                if (node.isChecked) add("marcado")
            }.joinToString(", ")

            buildString {
                append(content)
                role?.let { append(", $it") }
                if (states.isNotBlank()) append(", $states")
            }
        } finally {
            node.recycle()
        }
    }

    private fun roleFromClassName(className: String?): String? = when {
        className == null -> null
        "Button" in className -> "Botão"
        "EditText" in className -> "Campo de texto"
        "CheckBox" in className -> "Caixa de seleção"
        "Switch" in className -> "Interruptor"
        "RadioButton" in className -> "Botão de rádio"
        "ImageButton" in className -> "Botão"
        "SeekBar" in className -> "Controle deslizante"
        else -> null
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
        const val ANNOUNCE_TAG = "BlindCheckAnnounce"

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
