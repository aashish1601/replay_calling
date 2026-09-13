package com.memory.app.telecom

import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.memory.app.model.CallDirection
import com.memory.app.model.CallSession
import com.memory.app.model.CallState
import com.memory.app.recording.RecordingEngine
import com.memory.app.repository.CallRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MemoryInCallService : InCallService() {

    @Inject
    lateinit var callStateManager: CallStateManager

    @Inject
    lateinit var callRepository: CallRepository

    @Inject
    lateinit var recordingEngine: RecordingEngine
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "MemoryInCallService"
    }

    override fun onCreate() {
        super.onCreate()
        if (::callStateManager.isInitialized) {
            callStateManager.setInCallService(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::callStateManager.isInitialized) {
            callStateManager.setInCallService(null)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        if (::callStateManager.isInitialized) {
            callStateManager.updateAudioState(audioState.isMuted, audioState.route)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        if (::callStateManager.isInitialized) {
            callStateManager.setInCallService(this)
        }
        
        val callId = call.hashCode().toString()
        val handle = call.details.handle?.schemeSpecificPart ?: "Unknown"
        val callState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        
        val state = mapTelecomState(callState)
        val direction = if (callState == Call.STATE_RINGING) {
             CallDirection.INCOMING
        } else if (callState == Call.STATE_DIALING || callState == Call.STATE_CONNECTING) {
             CallDirection.OUTGOING
        } else {
             CallDirection.UNKNOWN
        }

        val session = CallSession(
            id = callId,
            phoneNumber = handle,
            contactName = call.details.callerDisplayName,
            direction = direction,
            state = state,
            startedAt = System.currentTimeMillis(),
            activeStartedAt = if (state == CallState.ACTIVE) System.currentTimeMillis() else null,
            telecomCall = call
        )
        
        callStateManager.addOrUpdateCall(session)
        
        call.registerCallback(callCallback)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        val callId = call.hashCode().toString()
        
        val session = callStateManager.getCall(callId)
        if (session != null) {
            val disconnectedSession = session.copy(state = CallState.DISCONNECTED)
            callStateManager.addOrUpdateCall(disconnectedSession)
            
            // Persist call history
            serviceScope.launch {
                callRepository.saveCall(disconnectedSession)
            }
            
            // Delegate disconnected handling to the recording engine
            val disconnectedEntity = com.memory.app.db.CallEntity(
                id = disconnectedSession.id.toLongOrNull() ?: System.currentTimeMillis(),
                phoneNumber = disconnectedSession.phoneNumber,
                contactName = disconnectedSession.contactName,
                direction = disconnectedSession.direction.name,
                startedAt = disconnectedSession.startedAt,
                endedAt = System.currentTimeMillis(),
                duration = System.currentTimeMillis() - disconnectedSession.startedAt
            )
            recordingEngine.onCallDisconnected(disconnectedEntity)
        }
        
        callStateManager.removeCall(callId)
        call.unregisterCallback(callCallback)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            val callId = call.hashCode().toString()
            val session = callStateManager.getCall(callId)
            if (session != null) {
                val newState = mapTelecomState(state)
                
                // If call becomes active, trigger recording
                if (newState == CallState.ACTIVE && session.state != CallState.ACTIVE) {
                    serviceScope.launch {
                        val useSpeaker = recordingEngine.onCallActive(callId)
                        if (useSpeaker) {
                            Log.d(TAG, "Forcing speakerphone for acoustic recording")
                            setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                        }
                    }
                }

                val activeStartedAt = if (newState == CallState.ACTIVE) {
                    session.activeStartedAt ?: System.currentTimeMillis()
                } else {
                    session.activeStartedAt
                }
                callStateManager.addOrUpdateCall(
                    session.copy(state = newState, activeStartedAt = activeStartedAt)
                )
            }
        }
        
        override fun onDetailsChanged(call: Call, details: Call.Details) {
            val callId = call.hashCode().toString()
            val session = callStateManager.getCall(callId)
            if (session != null) {
                val handle = details.handle?.schemeSpecificPart ?: session.phoneNumber
                val name = details.callerDisplayName ?: session.contactName
                callStateManager.addOrUpdateCall(
                    session.copy(phoneNumber = handle, contactName = name)
                )
            }
        }
    }

    private fun mapTelecomState(state: Int): CallState {
        return when (state) {
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallState.DIALING
            Call.STATE_RINGING -> CallState.RINGING
            Call.STATE_ACTIVE -> CallState.ACTIVE
            Call.STATE_DISCONNECTED -> CallState.DISCONNECTED
            else -> CallState.UNKNOWN
        }
    }
}
