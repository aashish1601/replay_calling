package com.memory.app.telecom

import android.telecom.CallAudioState
import com.memory.app.model.CallDirection
import com.memory.app.model.CallSession
import com.memory.app.model.CallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallStateManagerTest {

    private lateinit var callStateManager: CallStateManager

    @Before
    fun setUp() {
        callStateManager = CallStateManager()
    }

    @Test
    fun addCall_updatesActiveCallsAndCurrentCall() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "+1234567890",
            contactName = "Alice",
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            startedAt = System.currentTimeMillis()
        )

        callStateManager.addOrUpdateCall(session)

        assertEquals(1, callStateManager.activeCalls.value.size)
        assertEquals("call_1", callStateManager.currentCall.value?.id)
        assertEquals(CallState.RINGING, callStateManager.currentCall.value?.state)
    }

    @Test
    fun answerCall_updatesCallStateToActive() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "+1234567890",
            contactName = "Alice",
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            startedAt = System.currentTimeMillis()
        )
        callStateManager.addOrUpdateCall(session)

        callStateManager.answerCall("call_1")

        assertEquals(CallState.ACTIVE, callStateManager.currentCall.value?.state)
        assertNotNull(callStateManager.currentCall.value?.activeStartedAt)
    }

    @Test
    fun rejectCall_removesCallAndClearsCurrentCall() {
        val session = CallSession(
            id = "call_1",
            phoneNumber = "+1234567890",
            contactName = "Alice",
            direction = CallDirection.INCOMING,
            state = CallState.RINGING,
            startedAt = System.currentTimeMillis()
        )
        callStateManager.addOrUpdateCall(session)

        callStateManager.rejectCall("call_1")

        assertTrue(callStateManager.activeCalls.value.isEmpty())
        assertNull(callStateManager.currentCall.value)
    }

    @Test
    fun toggleMute_togglesMuteState() {
        assertFalse(callStateManager.isMuted.value)

        callStateManager.toggleMute()
        assertTrue(callStateManager.isMuted.value)

        callStateManager.toggleMute()
        assertFalse(callStateManager.isMuted.value)
    }

    @Test
    fun toggleSpeaker_togglesSpeakerStateAndAudioRoute() {
        assertFalse(callStateManager.isSpeakerOn.value)
        assertEquals(CallAudioState.ROUTE_EARPIECE, callStateManager.audioRoute.value)

        callStateManager.toggleSpeaker()
        assertTrue(callStateManager.isSpeakerOn.value)
        assertEquals(CallAudioState.ROUTE_SPEAKER, callStateManager.audioRoute.value)

        callStateManager.toggleSpeaker()
        assertFalse(callStateManager.isSpeakerOn.value)
        assertEquals(CallAudioState.ROUTE_EARPIECE, callStateManager.audioRoute.value)
    }

    @Test
    fun updateAudioState_updatesMuteAndSpeakerState() {
        callStateManager.updateAudioState(isMuted = true, route = CallAudioState.ROUTE_SPEAKER)

        assertTrue(callStateManager.isMuted.value)
        assertTrue(callStateManager.isSpeakerOn.value)
        assertEquals(CallAudioState.ROUTE_SPEAKER, callStateManager.audioRoute.value)
    }
}
