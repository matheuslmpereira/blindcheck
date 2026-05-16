package com.theustech.blindcheck_tracker

object TrackingServiceStatus {
    private const val TRACKING_PACKAGE = "com.theustech.blindcheck_tracking_app"
    private const val TRACKING_SERVICE_CLASS =
        "com.theustech.blindcheck_tracking_app.TrackingAccessibilityService"

    fun isTrackingService(packageName: String?, serviceClassName: String?): Boolean {
        return packageName == TRACKING_PACKAGE && serviceClassName == TRACKING_SERVICE_CLASS
    }
}
