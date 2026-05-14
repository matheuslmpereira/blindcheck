package com.theustech.blindcheck_tracking_app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingServiceStatusTest {
    @Test
    fun isTrackingService_matchesPackageAndClassName() {
        assertTrue(
            TrackingServiceStatus.isTrackingService(
                packageName = "com.theustech.blindcheck_tracking_app",
                serviceClassName = "com.theustech.blindcheck_tracking_app.TrackingAccessibilityService",
            ),
        )
        assertFalse(
            TrackingServiceStatus.isTrackingService(
                packageName = "com.theustech.blindcheck_tracking_app.extra",
                serviceClassName = "com.theustech.blindcheck_tracking_app.TrackingAccessibilityService",
            ),
        )
        assertFalse(
            TrackingServiceStatus.isTrackingService(
                packageName = "com.theustech.blindcheck_tracking_app",
                serviceClassName = "com.theustech.blindcheck_tracking_app.extra.TrackingAccessibilityService",
            ),
        )
    }

    @Test
    fun isTrackingService_returnsFalseForMissingServiceInfo() {
        assertFalse(
            TrackingServiceStatus.isTrackingService(
                packageName = null,
                serviceClassName = null,
            ),
        )
        assertFalse(
            TrackingServiceStatus.isTrackingService(
                packageName = "com.theustech.blindcheck_tracking_app",
                serviceClassName = null,
            ),
        )
    }
}
