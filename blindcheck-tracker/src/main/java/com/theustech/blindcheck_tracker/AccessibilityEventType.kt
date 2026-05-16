package com.theustech.blindcheck_tracker

import android.view.accessibility.AccessibilityEvent

enum class AccessibilityEventType(
    val androidName: String,
    val androidValue: Int,
    val serviceConfigName: String,
    val label: String,
) {
    ViewAccessibilityFocused(
        androidName = "TYPE_VIEW_ACCESSIBILITY_FOCUSED",
        androidValue = AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED,
        serviceConfigName = "typeViewAccessibilityFocused",
        label = "Accessibility focus",
    ),
    ViewAccessibilityFocusCleared(
        androidName = "TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED",
        androidValue = AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED,
        serviceConfigName = "typeViewAccessibilityFocusCleared",
        label = "Accessibility focus cleared",
    ),
    ViewFocused(
        androidName = "TYPE_VIEW_FOCUSED",
        androidValue = AccessibilityEvent.TYPE_VIEW_FOCUSED,
        serviceConfigName = "typeViewFocused",
        label = "View focused",
    ),
    ViewClicked(
        androidName = "TYPE_VIEW_CLICKED",
        androidValue = AccessibilityEvent.TYPE_VIEW_CLICKED,
        serviceConfigName = "typeViewClicked",
        label = "Click",
    ),
    ViewSelected(
        androidName = "TYPE_VIEW_SELECTED",
        androidValue = AccessibilityEvent.TYPE_VIEW_SELECTED,
        serviceConfigName = "typeViewSelected",
        label = "Selection",
    ),
    ViewTextChanged(
        androidName = "TYPE_VIEW_TEXT_CHANGED",
        androidValue = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
        serviceConfigName = "typeViewTextChanged",
        label = "Text changed",
    ),
    WindowStateChanged(
        androidName = "TYPE_WINDOW_STATE_CHANGED",
        androidValue = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        serviceConfigName = "typeWindowStateChanged",
        label = "Window state",
    ),
    WindowContentChanged(
        androidName = "TYPE_WINDOW_CONTENT_CHANGED",
        androidValue = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
        serviceConfigName = "typeWindowContentChanged",
        label = "Content changed",
    ),
    ViewScrolled(
        androidName = "TYPE_VIEW_SCROLLED",
        androidValue = AccessibilityEvent.TYPE_VIEW_SCROLLED,
        serviceConfigName = "typeViewScrolled",
        label = "Scroll",
    ),
    ViewTextSelectionChanged(
        androidName = "TYPE_VIEW_TEXT_SELECTION_CHANGED",
        androidValue = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
        serviceConfigName = "typeViewTextSelectionChanged",
        label = "Text selection",
    ),
    @Suppress("DEPRECATION")
    Announcement(
        androidName = "TYPE_ANNOUNCEMENT",
        androidValue = AccessibilityEvent.TYPE_ANNOUNCEMENT,
        serviceConfigName = "typeAnnouncement",
        label = "Announcement",
    ),
    NotificationStateChanged(
        androidName = "TYPE_NOTIFICATION_STATE_CHANGED",
        androidValue = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED,
        serviceConfigName = "typeNotificationStateChanged",
        label = "Notification",
    ),
    WindowsChanged(
        androidName = "TYPE_WINDOWS_CHANGED",
        androidValue = AccessibilityEvent.TYPE_WINDOWS_CHANGED,
        serviceConfigName = "typeWindowsChanged",
        label = "Windows changed",
    );

    companion object {
        fun fromAndroidName(androidName: String?): AccessibilityEventType? {
            return entries.firstOrNull { it.androidName == androidName }
        }
    }
}
