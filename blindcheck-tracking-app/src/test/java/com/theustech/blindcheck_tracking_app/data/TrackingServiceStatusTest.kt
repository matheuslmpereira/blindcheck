package com.theustech.blindcheck_tracking_app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingServiceStatusTest {
    @Test
    fun isTrackingServiceEnabled_returnsFalseForMissingSetting() {
        assertFalse(TrackingServiceStatus.isTrackingServiceEnabled(null))
        assertFalse(TrackingServiceStatus.isTrackingServiceEnabled(""))
    }

    @Test
    fun isTrackingServiceEnabled_matchesFullComponent() {
        assertTrue(
            TrackingServiceStatus.isTrackingServiceEnabled(
                "com.example/.Other:com.theustech.blindcheck_tracking_app/com.theustech.blindcheck_tracking_app.TrackingAccessibilityService",
            ),
        )
    }

    @Test
    fun isTrackingServiceEnabled_matchesShortComponent() {
        assertTrue(
            TrackingServiceStatus.isTrackingServiceEnabled(
                "com.example/.Other:com.theustech.blindcheck_tracking_app/.TrackingAccessibilityService",
            ),
        )
    }

    @Test
    fun isTrackingServiceEnabled_returnsFalseWhenOnlyTalkBackIsEnabled() {
        assertFalse(
            TrackingServiceStatus.isTrackingServiceEnabled(
                "com.google.android.marvin.talkback/.TalkBackService",
            ),
        )
    }

    @Test
    fun isTrackingServiceEnabled_doesNotMatchPrefixOnly() {
        assertFalse(
            TrackingServiceStatus.isTrackingServiceEnabled(
                "com.theustech.blindcheck_tracking_app.extra/.TrackingAccessibilityService",
            ),
        )
    }

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
    }
}
