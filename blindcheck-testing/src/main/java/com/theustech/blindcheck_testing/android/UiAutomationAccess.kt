package com.theustech.blindcheck_testing.android

import android.app.Instrumentation
import android.app.UiAutomation

/**
 * Returns the automation connection used by BlindCheck.
 *
 * The default connection suppresses every other accessibility service while instrumentation runs,
 * which silently disables TalkBack — exactly the service the screen-reader tests need alive.
 * Connecting with [UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES] keeps TalkBack bound.
 */
internal fun Instrumentation.blindCheckUiAutomation(): UiAutomation =
    getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
