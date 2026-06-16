package com.theustech.blindcheck_tracking_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

class BlindCheckTtsDataActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent.action) {
            TextToSpeech.Engine.ACTION_GET_SAMPLE_TEXT -> {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(
                        TextToSpeech.Engine.EXTRA_SAMPLE_TEXT,
                        "BlindCheck TTS capture sample",
                    ),
                )
            }
            else -> {
                setResult(
                    TextToSpeech.Engine.CHECK_VOICE_DATA_PASS,
                    Intent()
                        .putStringArrayListExtra(
                            TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                            arrayListOf("eng-USA", "por-BRA"),
                        )
                        .putStringArrayListExtra(
                            TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,
                            arrayListOf(),
                        ),
                )
            }
        }

        finish()
    }
}
