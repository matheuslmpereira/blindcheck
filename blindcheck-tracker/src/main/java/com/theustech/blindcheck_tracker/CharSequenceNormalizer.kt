package com.theustech.blindcheck_tracker

internal fun CharSequence?.normalizedString(): String? {
    return this?.toString()?.takeUnless { it.isBlank() }
}
