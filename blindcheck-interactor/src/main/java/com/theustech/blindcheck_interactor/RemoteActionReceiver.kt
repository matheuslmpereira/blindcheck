package com.theustech.blindcheck_interactor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.theustech.blindcheck_tracker.RemoteActions

class RemoteActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) return
        Log.d(TAG, "Received remote action: $action")
        TrackingAccessibilityService.executor?.execute(action)
    }

    companion object {
        private const val TAG = "BlindCheckRemote"
        private val SUPPORTED_ACTIONS = setOf(
            RemoteActions.ACTION_NEXT,
            RemoteActions.ACTION_PREVIOUS,
            RemoteActions.ACTION_ACTIVATE,
            RemoteActions.ACTION_BACK,
            RemoteActions.ACTION_SCROLL_FORWARD,
            RemoteActions.ACTION_SCROLL_BACKWARD,
            RemoteActions.ACTION_HOME,
            RemoteActions.ACTION_RECENTS,
            RemoteActions.ACTION_SWIPE_UP,
            RemoteActions.ACTION_SWIPE_DOWN,
        )
    }
}
