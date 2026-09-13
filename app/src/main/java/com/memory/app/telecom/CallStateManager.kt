package com.memory.app.telecom

import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.memory.app.model.CallSession
import com.memory.app.model.CallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallStateManager @Inject constructor() {
    private val _activeCalls = MutableStateFlow<List<CallSession>>(emptyList())
    val activeCalls: StateFlow<List<CallSession>> = _activeCalls.asStateFlow()

    private val _currentCall = MutableStateFlow<CallSession?>(null)
    val currentCall: StateFlow<CallSession?> = _currentCall.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _audioRoute = MutableStateFlow(CallAudioState.ROUTE_EARPIECE)
    val audioRoute: StateFlow<Int> = _audioRoute.asStateFlow()

    private var inCallService: InCallService? = null

    fun setInCallService(service: InCallService?) {
        this.inCallService = service
    }

    fun addOrUpdateCall(session: CallSession) {
        val currentCalls = _activeCalls.value.toMutableList()
        val index = currentCalls.indexOfFirst { it.id == session.id }

        val updatedSession = if (session.state == CallState.ACTIVE && session.activeStartedAt == null) {
            val existing = currentCalls.getOrNull(index)
            val startTime = existing?.activeStartedAt ?: System.currentTimeMillis()
            session.copy(activeStartedAt = startTime)
        } else {
            session
        }

        if (index != -1) {
            currentCalls[index] = updatedSession
        } else {
            currentCalls.add(updatedSession)
        }
        _activeCalls.value = currentCalls
        updateCurrentCall()
    }

    fun removeCall(callId: String) {
        _activeCalls.value = _activeCalls.value.filter { it.id != callId }
        updateCurrentCall()
    }

    fun getCall(callId: String): CallSession? {
        return _activeCalls.value.find { it.id == callId }
    }

    private fun updateCurrentCall() {
        val activeList = _activeCalls.value
        _currentCall.value = activeList.firstOrNull {
            it.state == CallState.RINGING || it.state == CallState.DIALING || it.state == CallState.ACTIVE
        } ?: activeList.firstOrNull { it.state != CallState.DISCONNECTED }
    }

    fun answerCall(callId: String? = null) {
        val targetCall = (if (callId != null) getCall(callId) else currentCall.value) ?: return
        try {
            targetCall.telecomCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        } catch (e: Exception) {
            // Telecom call failed or null
        }
        val updated = targetCall.copy(
            state = CallState.ACTIVE,
            activeStartedAt = System.currentTimeMillis()
        )
        addOrUpdateCall(updated)
    }

    fun rejectCall(callId: String? = null) {
        val targetCall = (if (callId != null) getCall(callId) else currentCall.value) ?: return
        try {
            if (targetCall.state == CallState.RINGING) {
                targetCall.telecomCall?.reject(false, null)
            } else {
                targetCall.telecomCall?.disconnect()
            }
        } catch (e: Exception) {
            // Telecom call failed or null
        }
        val updated = targetCall.copy(state = CallState.DISCONNECTED)
        addOrUpdateCall(updated)
        removeCall(targetCall.id)
    }

    fun disconnectCall(callId: String? = null) {
        val targetCall = (if (callId != null) getCall(callId) else currentCall.value) ?: return
        try {
            targetCall.telecomCall?.disconnect()
        } catch (e: Exception) {
            // Telecom call failed or null
        }
        val updated = targetCall.copy(state = CallState.DISCONNECTED)
        addOrUpdateCall(updated)
        removeCall(targetCall.id)
    }

    fun toggleMute() {
        setMuted(!_isMuted.value)
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        try {
            inCallService?.setMuted(muted)
        } catch (e: Exception) {
            // InCallService setMuted failed
        }
    }

    fun toggleSpeaker() {
        setSpeaker(!_isSpeakerOn.value)
    }

    fun setSpeaker(speaker: Boolean) {
        _isSpeakerOn.value = speaker
        val route = if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
        _audioRoute.value = route
        try {
            inCallService?.setAudioRoute(route)
        } catch (e: Exception) {
            // InCallService setAudioRoute failed
        }
    }

    fun updateAudioState(isMuted: Boolean, route: Int) {
        _isMuted.value = isMuted
        _audioRoute.value = route
        _isSpeakerOn.value = (route == CallAudioState.ROUTE_SPEAKER)
    }

    fun playDtmfTone(digit: Char) {
        currentCall.value?.telecomCall?.playDtmfTone(digit)
    }

    fun stopDtmfTone() {
        currentCall.value?.telecomCall?.stopDtmfTone()
    }
}
