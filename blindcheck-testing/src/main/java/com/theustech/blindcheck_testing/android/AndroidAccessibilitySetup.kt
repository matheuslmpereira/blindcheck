package com.theustech.blindcheck_testing.android

import android.app.Instrumentation
import android.os.SystemClock
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
 * [restoreAccessibilitySettings] allow opting in to a more realistic service environment
 * without leaving the device in a modified state.
 */
class AndroidAccessibilitySetup(
    private val instrumentation: Instrumentation,
) {
    class AccessibilitySettingsSnapshot internal constructor(
        val enabledAccessibilityServices: String?,
        val accessibilityEnabled: String?,
    )

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
     * Pair it with [captureAccessibilitySettings] and [restoreAccessibilitySettings]
     * when the previous device state must be preserved.
     */
    fun enableTalkBack() {
        shell("settings put secure enabled_accessibility_services $TALKBACK_SERVICE_ID")
        shell("settings put secure accessibility_enabled 1")
    }

    /** Waits for Android to bind the TalkBack service after [enableTalkBack]. */
    fun waitForTalkBackService(timeoutMs: Long = DEFAULT_TALKBACK_BIND_TIMEOUT_MS): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        do {
            if (shell("dumpsys accessibility").contains("Bound services:{Service[label=TalkBack")) {
                return true
            }
            SystemClock.sleep(TALKBACK_BIND_POLL_INTERVAL_MS)
        } while (SystemClock.uptimeMillis() < deadline)
        return false
    }

    /**
     * Disables TalkBack by clearing the enabled services list.
     */
    fun disableTalkBack() {
        shell("settings put secure enabled_accessibility_services ''")
        instrumentation.waitForIdleSync()
    }

    /** Returns whether the device has the Google TalkBack package required by opt-in tests. */
    fun isTalkBackInstalled(): Boolean = shell("cmd package path $TALKBACK_PACKAGE_NAME").isNotBlank()

    /**
     * Returns whether a test was explicitly requested through instrumentation arguments.
     * This prevents TalkBack-dependent tests from changing an arbitrary connected device.
     */
    fun isTalkBackFocusTestEnabled(): Boolean {
        return InstrumentationRegistry.getArguments().getString(TALKBACK_FOCUS_TEST_ARGUMENT) == "true"
    }

    /** Captures the settings changed by [enableTalkBack] so callers can restore them in @After. */
    fun captureAccessibilitySettings(): AccessibilitySettingsSnapshot {
        return AccessibilitySettingsSnapshot(
            enabledAccessibilityServices = secureSetting("enabled_accessibility_services"),
            accessibilityEnabled = secureSetting("accessibility_enabled"),
        )
    }

    /** Restores settings captured by [captureAccessibilitySettings]. */
    fun restoreAccessibilitySettings(snapshot: AccessibilitySettingsSnapshot) {
        restoreSecureSetting("enabled_accessibility_services", snapshot.enabledAccessibilityServices)
        restoreSecureSetting("accessibility_enabled", snapshot.accessibilityEnabled)
    }

    private fun secureSetting(name: String): String? {
        return shell("settings get secure $name")
            .trim()
            .takeUnless { it.isBlank() || it == "null" }
    }

    private fun restoreSecureSetting(name: String, value: String?) {
        if (value == null) {
            shell("settings delete secure $name")
        } else {
            shell("settings put secure $name ${shellQuote(value)}")
        }
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\\"'\\\"'")}'"

    private fun shell(command: String): String {
        val pfd = instrumentation.blindCheckUiAutomation().executeShellCommand(command)
        return pfd.use {
            // Drain the output so the command is guaranteed to complete before we continue.
            FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes().decodeToString() }
        }
    }

    companion object {
        const val TALKBACK_FOCUS_TEST_ARGUMENT = "runTalkBackFocusTests"
        const val TALKBACK_PACKAGE_NAME = "com.google.android.marvin.talkback"
        const val TALKBACK_SERVICE_ID =
            "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
        // TalkBack is restarted by every instrumentation install, so a cold bind can take
        // well over five seconds. A short timeout turned opt-in tests into silent skips.
        private const val DEFAULT_TALKBACK_BIND_TIMEOUT_MS = 30_000L
        private const val TALKBACK_BIND_POLL_INTERVAL_MS = 100L

        fun create(): AndroidAccessibilitySetup =
            AndroidAccessibilitySetup(InstrumentationRegistry.getInstrumentation())
    }
}
