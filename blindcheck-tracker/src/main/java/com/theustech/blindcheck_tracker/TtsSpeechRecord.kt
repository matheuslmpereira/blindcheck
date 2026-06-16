package com.theustech.blindcheck_tracker

data class TtsSpeechRecord(
    val id: String,
    val timestamp: Long,
    val text: String,
    val language: String?,
    val country: String?,
    val variant: String?,
    val voiceName: String?,
    val speechRate: Int,
    val pitch: Int,
)
