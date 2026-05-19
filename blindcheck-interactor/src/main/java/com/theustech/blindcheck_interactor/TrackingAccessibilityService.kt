package com.theustech.blindcheck_interactor

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.theustech.blindcheck_tracker.A11yEventNormalizer
import com.theustech.blindcheck_tracker.RemoteActions
import com.theustech.blindcheck_tracker.TrackingEventStore

class TrackingAccessibilityService : AccessibilityService(), ActionExecutor {

    private val normalizer = A11yEventNormalizer()
    private val eventStore = TrackingEventStore.shared

    // Texts recently announced via FOCUS — used to suppress duplicate ANN entries.
    @Volatile private var lastFocusTexts: Set<String> = emptySet()

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
                lastFocusTexts = msg.split(", ").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
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
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val node = event.source ?: return
                try {
                    val hasError     = node.error != null
                    val isLiveRegion = node.liveRegion != 0
                    // Also handle the supporting-text container: a View whose parent EditText has an error.
                    val parentHasError = if (!hasError && !isLiveRegion) {
                        val parent = node.parent
                        try { parent?.error != null } finally { parent?.recycle() }
                    } else false

                    if (!hasError && !isLiveRegion && !parentHasError) return

                    // When parentHasError: this node IS the supporting-text container → text is in its children.
                    // When hasError: error text is in grandchildren (supporting-text slot is a child View).
                    val msgs = mutableListOf<String>()
                    fun nodeText(n: AccessibilityNodeInfo): String? {
                        val t = n.text?.toString()?.trim()
                        val c = n.contentDescription?.toString()?.trim()
                        return t?.takeIf { it.isNotBlank() } ?: c?.takeIf { it.isNotBlank() }
                    }
                    // Collect from direct children and grandchildren (error text location varies).
                    for (i in 0 until node.childCount) {
                        val child = node.getChild(i) ?: continue
                        try {
                            nodeText(child)?.let { msgs.add(it) }
                            for (j in 0 until child.childCount) {
                                val gc = child.getChild(j) ?: continue
                                try { nodeText(gc)?.let { msgs.add(it) } } finally { gc.recycle() }
                            }
                        } finally { child.recycle() }
                    }
                    val seen = lastFocusTexts
                    msgs.filter { it !in seen }.forEach { Log.i(ANNOUNCE_TAG, "ANN $it") }
                } finally {
                    node.recycle()
                }
            }
        }
    }

    private fun buildFocusMessage(event: AccessibilityEvent): String? {
        val node = event.source ?: return null
        return try {
            val text = node.text?.toString()?.trim()
            val cd   = node.contentDescription?.toString()?.trim()

            // Compose places label text and role in child nodes; error text may be a grandchild.
            val childTexts = mutableListOf<String>()
            var childRole: String? = null
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val ct  = child.text?.toString()?.trim()
                    val ccd = child.contentDescription?.toString()?.trim()
                    (ct?.takeIf { it.isNotBlank() } ?: ccd?.takeIf { it.isNotBlank() })
                        ?.let { childTexts.add(it) }
                    if (childRole == null) childRole = roleFromClassName(child.className?.toString())
                    // One level deeper — supporting/error text lives here in Material3 TextField
                    for (j in 0 until child.childCount) {
                        val gc = child.getChild(j) ?: continue
                        try {
                            val gct  = gc.text?.toString()?.trim()
                            val gccd = gc.contentDescription?.toString()?.trim()
                            (gct?.takeIf { it.isNotBlank() } ?: gccd?.takeIf { it.isNotBlank() })
                                ?.let { if (!childTexts.contains(it)) childTexts.add(it) }
                        } finally { gc.recycle() }
                    }
                } finally {
                    child.recycle()
                }
            }
            val childText = childTexts.joinToString(", ").takeIf { it.isNotBlank() }

            val role = roleFromClassName(node.className?.toString()) ?: childRole
            val content = when {
                !text.isNullOrBlank() && !cd.isNullOrBlank() -> "$text, $cd"
                !text.isNullOrBlank()  -> text
                !cd.isNullOrBlank()    -> cd
                !childText.isNullOrBlank() -> childText!!
                event.text.isNotEmpty() -> event.text.joinToString(", ")
                role != null -> ""
                else -> return null
            }

            val error  = node.error?.toString()?.trim()
            val states = buildList {
                if (!node.isEnabled) add("desabilitado")
                if (node.isEditable) add("editável")
                if (node.isChecked) add("marcado")
            }.joinToString(", ")

            buildString {
                append(content)
                if (content.isNotBlank()) role?.let { append(", $it") } else role?.let { append(it) }
                if (states.isNotBlank()) append(", $states")
                if (!error.isNullOrBlank()) append(", Erro: $error")
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
