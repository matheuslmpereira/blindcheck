package com.theustech.blindcheck_tracking_app.data

object TrackingServiceStatus {
    private const val TRACKING_PACKAGE = "com.theustech.blindcheck_tracking_app"
    private const val TRACKING_SERVICE_CLASS = "com.theustech.blindcheck_tracking_app.TrackingAccessibilityService"
    private const val TRACKING_SERVICE_COMPONENT = "$TRACKING_PACKAGE/$TRACKING_SERVICE_CLASS"
    private const val TRACKING_SERVICE_SHORT_COMPONENT = "$TRACKING_PACKAGE/.TrackingAccessibilityService"

    fun isTrackingService(packageName: String?, serviceClassName: String?): Boolean {
        return packageName == TRACKING_PACKAGE && serviceClassName == TRACKING_SERVICE_CLASS
    }

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
