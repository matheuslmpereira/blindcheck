package com.theustech.blindcheck_tracking_app.data

internal fun CharSequence?.normalizedString(): String? {
    return this?.toString()?.takeUnless { it.isEmpty() }
}
