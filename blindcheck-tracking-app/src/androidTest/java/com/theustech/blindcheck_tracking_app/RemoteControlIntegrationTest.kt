package com.theustech.blindcheck_tracking_app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.theustech.blindcheck_tracker.RemoteActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Verifies the full remote control pipeline:
 * ADB broadcast → RemoteActionReceiver → ActionExecutor.execute()
 *
 * Sends a real broadcast through the Android system and checks both that the
 * executor was called and that the expected log entry appears in logcat.
 */
@RunWith(AndroidJUnit4::class)
class RemoteControlIntegrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun clearLogcat() {
        instrumentation.uiAutomation.executeShellCommand("logcat -c").close()
    }

    @After
    fun resetExecutor() {
        TrackingAccessibilityService.setExecutorForTest(null)
    }

    @Test
    fun actionNext_broadcastDelivered_executorCalledAndLogged() {
        assertBroadcastExecuted(RemoteActions.ACTION_NEXT)
    }

    @Test
    fun actionPrevious_broadcastDelivered_executorCalledAndLogged() {
        assertBroadcastExecuted(RemoteActions.ACTION_PREVIOUS)
    }

    @Test
    fun actionActivate_broadcastDelivered_executorCalledAndLogged() {
        assertBroadcastExecuted(RemoteActions.ACTION_ACTIVATE)
    }

    @Test
    fun actionBack_broadcastDelivered_executorCalledAndLogged() {
        assertBroadcastExecuted(RemoteActions.ACTION_BACK)
    }

    @Test
    fun actionScrollForward_broadcastDelivered_executorCalledAndLogged() {
        assertBroadcastExecuted(RemoteActions.ACTION_SCROLL_FORWARD)
    }

    @Test
    fun actionScrollBackward_broadcastDelivered_executorCalledAndLogged() {
        assertBroadcastExecuted(RemoteActions.ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun unknownAction_broadcastIgnored_executorNotCalled() {
        val latch = CountDownLatch(1)
        TrackingAccessibilityService.setExecutorForTest { latch.countDown() }

        context.sendBroadcast(
            Intent("com.unknown.ACTION_FOO").setPackage(context.packageName)
        )

        val triggered = latch.await(1, TimeUnit.SECONDS)
        assertTrue("Unknown action should not trigger the executor", !triggered)
    }

    // ---- helpers ----

    private fun assertBroadcastExecuted(action: String) {
        val latch = CountDownLatch(1)
        var received: String? = null

        TrackingAccessibilityService.setExecutorForTest { a ->
            received = a
            latch.countDown()
        }

        context.sendBroadcast(Intent(action).setPackage(context.packageName))

        assertTrue(
            "Action '$action' not received within 3s — is the package installed?",
            latch.await(3, TimeUnit.SECONDS)
        )
        assertEquals(action, received)

        assertLogContains(action)
    }

    private fun assertLogContains(action: String) {
        Thread.sleep(300) // give logcat time to flush
        val pfd = instrumentation.uiAutomation.executeShellCommand(
            "logcat -d -s BlindCheckRemote:D"
        )
        val log = FileInputStream(pfd.fileDescriptor).bufferedReader().readText()
        pfd.close()
        assertTrue(
            "Expected logcat entry for '$action' under tag BlindCheckRemote, got:\n$log",
            log.contains(action)
        )
    }
}
