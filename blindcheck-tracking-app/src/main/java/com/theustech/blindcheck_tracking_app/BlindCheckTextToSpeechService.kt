package com.theustech.blindcheck_tracking_app

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.theustech.blindcheck_tracker.TtsSpeechStore
import java.util.Locale

class BlindCheckTextToSpeechService : TextToSpeechService() {
    @Volatile
    private var stopRequested = false

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return when {
            lang.isNullOrBlank() -> TextToSpeech.LANG_AVAILABLE
            country.isNullOrBlank() -> TextToSpeech.LANG_AVAILABLE
            variant.isNullOrBlank() -> TextToSpeech.LANG_COUNTRY_AVAILABLE
            else -> TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onGetVoices(): MutableList<Voice> {
        return supportedLocales()
            .map { locale ->
                Voice(
                    locale.toLanguageTag(),
                    locale,
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_LOW,
                    false,
                    emptySet(),
                )
            }
            .toMutableList()
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String {
        return findSupportedLocale(lang, country, variant)?.toLanguageTag()
            ?: Locale.US.toLanguageTag()
    }

    override fun onLoadVoice(voiceName: String?): Int {
        return if (isSupportedVoiceName(voiceName)) {
            TextToSpeech.SUCCESS
        } else {
            TextToSpeech.ERROR
        }
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        return onLoadVoice(voiceName)
    }

    override fun onGetLanguage(): Array<String> {
        val locale = Locale.getDefault()
        return arrayOf(
            locale.safeIso3Language(),
            locale.safeIso3Country(),
            locale.variant.orEmpty(),
        )
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopRequested = false
        val text = request.charSequenceText
        if (!text.isOperationalStatusAnnouncement()) {
            val record = TtsSpeechStore.shared.record(
                text = text,
                language = request.language,
                country = request.country,
                variant = request.variant,
                voiceName = request.voiceName,
                speechRate = request.speechRate,
                pitch = request.pitch,
            )

            record?.let {
                Log.i(ANNOUNCE_TAG, "TTS ${it.text}")
            }
        }

        if (stopRequested) {
            callback.done()
            return
        }

        val started = callback.start(
            SAMPLE_RATE_HZ,
            AudioFormat.ENCODING_PCM_16BIT,
            CHANNEL_COUNT_MONO,
        )
        if (started == TextToSpeech.ERROR) return

        if (!stopRequested) {
            writeSilentAudio(text, callback)
        }
        callback.done()
    }

    override fun onStop() {
        stopRequested = true
    }

    private fun Locale.safeIso3Language(): String = try {
        isO3Language
    } catch (_: Exception) {
        language.orEmpty()
    }

    private fun Locale.safeIso3Country(): String = try {
        isO3Country
    } catch (_: Exception) {
        country.orEmpty()
    }

    private fun writeSilentAudio(text: CharSequence?, callback: SynthesisCallback) {
        val textLength = text?.length ?: 0
        val durationMs = (textLength * MILLIS_PER_CHARACTER)
            .coerceIn(MIN_UTTERANCE_MS, MAX_UTTERANCE_MS)
        var remainingBytes = ((SAMPLE_RATE_HZ * BYTES_PER_SAMPLE * durationMs) / 1_000)
            .coerceAtLeast(BYTES_PER_SAMPLE)
        if (remainingBytes % BYTES_PER_SAMPLE != 0) remainingBytes += 1

        while (!stopRequested && remainingBytes > 0) {
            val chunkSize = minOf(SILENCE_CHUNK.size, remainingBytes)
            val result = callback.audioAvailable(SILENCE_CHUNK, 0, chunkSize)
            if (result == TextToSpeech.ERROR) return
            remainingBytes -= chunkSize
        }
    }

    private fun supportedLocales(): List<Locale> {
        return listOf(
            Locale.US,
            Locale.Builder().setLanguage("pt").setRegion("BR").build(),
            Locale.getDefault(),
        ).distinctBy { it.toLanguageTag() }
    }

    private fun findSupportedLocale(lang: String?, country: String?, variant: String?): Locale? {
        return supportedLocales().firstOrNull { locale ->
            locale.matchesIso3(lang, country, variant)
        }
    }

    private fun Locale.matchesIso3(lang: String?, country: String?, variant: String?): Boolean {
        val languageMatches = lang.isNullOrBlank() ||
            lang == language ||
            lang == safeIso3Language()
        val countryMatches = country.isNullOrBlank() ||
            country == this.country ||
            country == safeIso3Country()
        val variantMatches = variant.isNullOrBlank() || variant == this.variant
        return languageMatches && countryMatches && variantMatches
    }

    private fun isSupportedVoiceName(voiceName: String?): Boolean {
        if (voiceName.isNullOrBlank()) return false
        return supportedLocales().any { it.toLanguageTag() == voiceName }
    }

    private fun CharSequence?.isOperationalStatusAnnouncement(): Boolean {
        return this?.toString()?.trim() in OPERATIONAL_STATUS_ANNOUNCEMENTS
    }

    companion object {
        const val ANNOUNCE_TAG = "BlindCheckAnnounce"
        private val OPERATIONAL_STATUS_ANNOUNCEMENTS = setOf(
            "Using BlindCheck TTS capture",
            "TalkBack off",
            "TalkBack on",
        )
        private const val SAMPLE_RATE_HZ = 16_000
        private const val CHANNEL_COUNT_MONO = 1
        private const val BYTES_PER_SAMPLE = 2
        private const val MILLIS_PER_CHARACTER = 6
        private const val MIN_UTTERANCE_MS = 80
        private const val MAX_UTTERANCE_MS = 450
        private val SILENCE_CHUNK = ByteArray(1_600)
    }
}
