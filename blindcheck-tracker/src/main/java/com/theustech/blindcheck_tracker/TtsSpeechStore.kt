package com.theustech.blindcheck_tracker

import java.util.UUID

class TtsSpeechStore(
    private val maxRecords: Int = DEFAULT_MAX_RECORDS,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val records = mutableListOf<TtsSpeechRecord>()

    @Synchronized
    fun record(
        text: CharSequence?,
        language: String?,
        country: String?,
        variant: String?,
        voiceName: String?,
        speechRate: Int,
        pitch: Int,
    ): TtsSpeechRecord? {
        val normalizedText = text?.toString()?.trim()?.takeUnless { it.isBlank() } ?: return null
        val record = TtsSpeechRecord(
            id = idProvider(),
            timestamp = clock(),
            text = normalizedText,
            language = language.normalizedOrNull(),
            country = country.normalizedOrNull(),
            variant = variant.normalizedOrNull(),
            voiceName = voiceName.normalizedOrNull(),
            speechRate = speechRate,
            pitch = pitch,
        )
        records += record
        trimToMaxRecords()
        return record
    }

    @Synchronized
    fun snapshot(): List<TtsSpeechRecord> = records.toList()

    @Synchronized
    fun clear() {
        records.clear()
    }

    private fun trimToMaxRecords() {
        while (records.size > maxRecords) {
            records.removeAt(0)
        }
    }

    private fun String?.normalizedOrNull(): String? = this?.trim()?.takeUnless { it.isBlank() }

    companion object {
        private const val DEFAULT_MAX_RECORDS = 500

        val shared = TtsSpeechStore()
    }
}
