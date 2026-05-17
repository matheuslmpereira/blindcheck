package com.theustech.blindcheck_tracking_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.theustech.blindcheck_tracker.RemoteActions

class RemoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        TrackingAccessibilityService.executor?.execute(action)
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            RemoteActions.ACTION_NEXT,
            RemoteActions.ACTION_PREVIOUS,
            RemoteActions.ACTION_ACTIVATE,
            RemoteActions.ACTION_BACK,
            RemoteActions.ACTION_SCROLL_FORWARD,
            RemoteActions.ACTION_SCROLL_BACKWARD,
        )
    }
}
