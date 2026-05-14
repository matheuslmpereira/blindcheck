package com.theustech.blindcheck_tracking_app.data

object TrackingServiceStatus {
    const val TRACKING_PACKAGE = "com.theustech.blindcheck_tracking_app"
    const val TRACKING_SERVICE_CLASS = "com.theustech.blindcheck_tracking_app.TrackingAccessibilityService"
    const val TRACKING_SERVICE_COMPONENT = "$TRACKING_PACKAGE/$TRACKING_SERVICE_CLASS"
    const val TRACKING_SERVICE_SHORT_COMPONENT = "$TRACKING_PACKAGE/.TrackingAccessibilityService"

    fun isTrackingServiceEnabled(enabledServicesSetting: String?): Boolean {
        if (enabledServicesSetting.isNullOrBlank()) return false

        return enabledServicesSetting
            .split(':')
            .any { component ->
                component == TRACKING_SERVICE_COMPONENT ||
                    component == TRACKING_SERVICE_SHORT_COMPONENT
            }
    }
}
