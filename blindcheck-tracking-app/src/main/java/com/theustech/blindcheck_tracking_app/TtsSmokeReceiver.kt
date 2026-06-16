package com.theustech.blindcheck_tracking_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log

class TtsSmokeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val text = intent.getStringExtra(EXTRA_TEXT)?.takeIf { it.isNotBlank() }
            ?: "BlindCheck TTS smoke test"
        val enginePackage = intent.getStringExtra(EXTRA_ENGINE_PACKAGE)?.takeIf { it.isNotBlank() }
        val appContext = context.applicationContext
        val mainHandler = Handler(Looper.getMainLooper())
        val holder = arrayOfNulls<TextToSpeech>(1)

        val listener = TextToSpeech.OnInitListener { status ->
            val tts = holder[0]
            if (status == TextToSpeech.SUCCESS && tts != null) {
                Log.i(
                    BlindCheckTextToSpeechService.ANNOUNCE_TAG,
                    "TTS_SMOKE_REQUEST ${enginePackage ?: "default"} $text",
                )
                tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    Bundle(),
                    "blindcheck-smoke-${System.currentTimeMillis()}",
                )
            } else {
                Log.i(BlindCheckTextToSpeechService.ANNOUNCE_TAG, "TTS_SMOKE_INIT_ERROR $status")
            }

            mainHandler.postDelayed(
                {
                    holder[0]?.shutdown()
                    pendingResult.finish()
                },
                SHUTDOWN_DELAY_MS,
            )
        }

        holder[0] = if (enginePackage == null) {
            TextToSpeech(appContext, listener)
        } else {
            TextToSpeech(appContext, listener, enginePackage)
        }
    }

    companion object {
        const val ACTION_TTS_SMOKE = "com.theustech.blindcheck.ACTION_TTS_SMOKE"
        const val EXTRA_TEXT = "com.theustech.blindcheck.EXTRA_TTS_TEXT"
        const val EXTRA_ENGINE_PACKAGE = "com.theustech.blindcheck.EXTRA_TTS_ENGINE_PACKAGE"
        private const val SHUTDOWN_DELAY_MS = 1_000L
    }
}
