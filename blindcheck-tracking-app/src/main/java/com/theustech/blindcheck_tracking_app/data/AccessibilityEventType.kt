package com.theustech.blindcheck_tracking_app.data

import android.view.accessibility.AccessibilityEvent

enum class AccessibilityEventType(
    val androidName: String,
    val androidValue: Int,
    val serviceConfigName: String,
) {
    ViewAccessibilityFocused(
        androidName = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
        androidValue = AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
        serviceConfigName = "typeViewAccessibilityFocused",
    ),
    ViewAccessibilityFocusCleared(
        androidName = "TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED",
        androidValue = AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
        serviceConfigName = "typeViewAccessibilityFocusCleared",
    ),
    ViewFocused(
        androidName = "TYPE_VIEW_FOCUSED",
        androidValue = AccessibilityEvent.TYPE_VIEW_FOCUSED,
        serviceConfigName = "typeViewFocused",
    ),
    ViewClicked(
        androidName = "TYPE_VIEW_CLICKED",
        androidValue = AccessibilityEvent.TYPE_VIEW_CLICKED,
        serviceConfigName = "typeViewClicked",
    ),
    ViewSelected(
        androidName = "TYPE_VIEW_SELECTED",
        androidValue = AccessibilityEvent.TYPE_VIEW_SELECTED,
        serviceConfigName = "typeViewSelected",
    ),
    ViewTextChanged(
        androidName = "TYPE_VIEW_TEXT_CHANGED",
        androidValue = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
        serviceConfigName = "typeViewTextChanged",
    ),
    WindowStateChanged(
        androidName = "TYPE_WINDOW_STATE_CHANGED",
        androidValue = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        serviceConfigName = "typeWindowStateChanged",
    ),
    WindowContentChanged(
        androidName = "TYPE_WINDOW_CONTENT_CHANGED",
        androidValue = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        serviceConfigName = "typeWindowContentChanged",
    ),
    ViewScrolled(
        androidName = "TYPE_VIEW_SCROLLED",
        androidValue = AccessibilityEvent.TYPE_VIEW_SCROLLED,
        serviceConfigName = "typeViewScrolled",
    ),
    ViewTextSelectionChanged(
        androidName = "TYPE_VIEW_TEXT_SELECTION_CHANGED",
        androidValue = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
        serviceConfigName = "typeViewTextSelectionChanged",
    ),
    @Suppress("DEPRECATION")
    Announcement(
        androidName = "TYPE_ANNOUNCEMENT",
        androidValue = AccessibilityEvent.TYPE_ANNOUNCEMENT,
        serviceConfigName = "typeAnnouncement",
    ),
    NotificationStateChanged(
        androidName = "TYPE_NOTIFICATION_STATE_CHANGED",
        androidValue = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED,
        serviceConfigName = "typeNotificationStateChanged",
    ),
    WindowsChanged(
        androidName = "TYPE_WINDOWS_CHANGED",
        androidValue = AccessibilityEvent.TYPE_WINDOWS_CHANGED,
        serviceConfigName = "typeWindowsChanged",
    );

    companion object {
        fun fromAndroidName(androidName: String?): AccessibilityEventType? {
            return entries.firstOrNull { it.androidName == androidName }
        }
    }
}
