package com.theustech.blindcheck_tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsSpeechStoreTest {
    @Test
    fun record_preservesCapturedTextAndRequestMetadata() {
        val store = TtsSpeechStore(
            idProvider = { "speech-1" },
            clock = { 123L },
        )

        val record = store.record(
            text = "  Entrar, botao  ",
            language = "por",
            country = "BRA",
            variant = "",
            voiceName = "blindcheck-por-bra",
            speechRate = 100,
            pitch = 90,
        )

        assertEquals("speech-1", record?.id)
        assertEquals(123L, record?.timestamp)
        assertEquals("Entrar, botao", record?.text)
        assertEquals("por", record?.language)
        assertEquals("BRA", record?.country)
        assertNull(record?.variant)
        assertEquals("blindcheck-por-bra", record?.voiceName)
        assertEquals(100, record?.speechRate)
        assertEquals(90, record?.pitch)
        assertEquals(listOf(record), store.snapshot())
    }

    @Test
    fun record_ignoresBlankText() {
        val store = TtsSpeechStore()

        val record = store.record(
            text = "  ",
            language = null,
            country = null,
            variant = null,
            voiceName = null,
            speechRate = 100,
            pitch = 100,
        )

        assertNull(record)
        assertTrue(store.snapshot().isEmpty())
    }

    @Test
    fun record_trimsOldestRecordsWhenLimitIsReached() {
        var nextId = 0
        val store = TtsSpeechStore(
            maxRecords = 2,
            idProvider = { "speech-${nextId++}" },
            clock = { nextId.toLong() },
        )

        store.record("first", null, null, null, null, 100, 100)
        store.record("second", null, null, null, null, 100, 100)
        store.record("third", null, null, null, null, 100, 100)

        assertEquals(listOf("second", "third"), store.snapshot().map { it.text })
    }

    @Test
    fun snapshot_returnsCopy() {
        val store = TtsSpeechStore()
        store.record("first", null, null, null, null, 100, 100)

        val snapshot = store.snapshot().toMutableList()
        snapshot.clear()

        assertEquals(listOf("first"), store.snapshot().map { it.text })
    }

    @Test
    fun clear_removesCapturedSpeech() {
        val store = TtsSpeechStore()
        store.record("first", null, null, null, null, 100, 100)

        store.clear()

        assertTrue(store.snapshot().isEmpty())
    }
}
