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
                "com.example/.Other:${TrackingServiceStatus.TRACKING_SERVICE_COMPONENT}",
            ),
        )
    }

    @Test
    fun isTrackingServiceEnabled_matchesShortComponent() {
        assertTrue(
            TrackingServiceStatus.isTrackingServiceEnabled(
                "com.example/.Other:${TrackingServiceStatus.TRACKING_SERVICE_SHORT_COMPONENT}",
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
}
