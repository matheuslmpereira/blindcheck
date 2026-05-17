package com.theustech.blindcheck_tracking_app

import android.content.Context
import android.content.Intent
import com.theustech.blindcheck_tracker.RemoteActions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RemoteActionReceiverTest {

    private val receiver = RemoteActionReceiver()
    private val context: Context = mock(Context::class.java)

    private val executedActions = mutableListOf<String>()
    private val fakeExecutor = ActionExecutor { action -> executedActions.add(action) }

    @Before
    fun setUp() {
        TrackingAccessibilityService.executor = fakeExecutor
    }

    @After
    fun tearDown() {
        TrackingAccessibilityService.executor = null
        executedActions.clear()
    }

    @Test
    fun `ACTION_NEXT broadcast calls execute with next action`() {
        receiver.onReceive(context, Intent(RemoteActions.ACTION_NEXT))
        assertEquals(listOf(RemoteActions.ACTION_NEXT), executedActions)
    }

    @Test
    fun `ACTION_PREVIOUS broadcast calls execute with previous action`() {
        receiver.onReceive(context, Intent(RemoteActions.ACTION_PREVIOUS))
        assertEquals(listOf(RemoteActions.ACTION_PREVIOUS), executedActions)
    }

    @Test
    fun `ACTION_ACTIVATE broadcast calls execute with activate action`() {
        receiver.onReceive(context, Intent(RemoteActions.ACTION_ACTIVATE))
        assertEquals(listOf(RemoteActions.ACTION_ACTIVATE), executedActions)
    }

    @Test
    fun `ACTION_BACK broadcast calls execute with back action`() {
        receiver.onReceive(context, Intent(RemoteActions.ACTION_BACK))
        assertEquals(listOf(RemoteActions.ACTION_BACK), executedActions)
    }

    @Test
    fun `ACTION_SCROLL_FORWARD broadcast calls execute with scroll forward action`() {
        receiver.onReceive(context, Intent(RemoteActions.ACTION_SCROLL_FORWARD))
        assertEquals(listOf(RemoteActions.ACTION_SCROLL_FORWARD), executedActions)
    }

    @Test
    fun `ACTION_SCROLL_BACKWARD broadcast calls execute with scroll backward action`() {
        receiver.onReceive(context, Intent(RemoteActions.ACTION_SCROLL_BACKWARD))
        assertEquals(listOf(RemoteActions.ACTION_SCROLL_BACKWARD), executedActions)
    }

    @Test
    fun `unknown action broadcast is ignored`() {
        receiver.onReceive(context, Intent("com.unknown.ACTION_FOO"))
        assertTrue(executedActions.isEmpty())
    }

    @Test
    fun `null action broadcast is ignored`() {
        receiver.onReceive(context, Intent())
        assertTrue(executedActions.isEmpty())
    }

    @Test
    fun `broadcast with no executor does not crash`() {
        TrackingAccessibilityService.executor = null
        receiver.onReceive(context, Intent(RemoteActions.ACTION_NEXT))
        assertTrue(executedActions.isEmpty())
    }
}
