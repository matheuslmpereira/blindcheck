package com.theustech.blindcheck_testing.android

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

/**
 * Encapsulates ADB shell commands needed to configure the Android accessibility pipeline
 * before running blind-user flow tests.
 *
 * TalkBack is not required for [AndroidAccessibilityTestDriver] to operate — UiAutomation
 * has direct access to the accessibility tree at all times. However, calling
 * [ensureAccessibilityEnabled] documents intent and ensures the secure setting is
 * consistent with what a real user session would have active.
 *
 * For tests on physical devices where TalkBack is available, [enableTalkBack] and
 * [disableTalkBack] allow opting in to a more realistic service environment.
 * Always call [disableTalkBack] in @After to avoid leaving the device in a modified state.
 */
class AndroidAccessibilitySetup(
    private val instrumentation: Instrumentation,
) {
    /**
     * Ensures the Android accessibility pipeline is marked as enabled in secure settings.
     * Safe to call on any emulator; has no effect if already enabled.
     */
    fun ensureAccessibilityEnabled() {
        shell("settings put secure accessibility_enabled 1")
        instrumentation.waitForIdleSync()
    }

    /**
     * Enables the TalkBack accessibility service.
     * Requires TalkBack to be installed on the device or emulator.
     * Call [disableTalkBack] in @After to restore state.
     */
    fun enableTalkBack() {
        shell("settings put secure enabled_accessibility_services $TALKBACK_SERVICE_ID")
        shell("settings put secure accessibility_enabled 1")
        instrumentation.waitForIdleSync()
    }

    /**
     * Disables TalkBack by clearing the enabled services list.
     */
    fun disableTalkBack() {
        shell("settings put secure enabled_accessibility_services ''")
        instrumentation.waitForIdleSync()
    }

    private fun shell(command: String) {
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        pfd.use {
            // Drain the output so the command is guaranteed to complete before we continue.
            FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
        }
    }

    companion object {
        const val TALKBACK_SERVICE_ID =
            "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"

        fun create(): AndroidAccessibilitySetup =
            AndroidAccessibilitySetup(InstrumentationRegistry.getInstrumentation())
    }
}
