package com.theustech.blindcheck_testing.actions

interface UserAccessibilityActions {
    suspend fun next()
    suspend fun previous()
    suspend fun activate()
    suspend fun scrollForward()
    suspend fun scrollBackward()
    suspend fun inputText(value: String)
    suspend fun back()
}
