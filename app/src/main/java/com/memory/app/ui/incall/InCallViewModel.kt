package com.memory.app.ui.incall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memory.app.model.CallSession
import com.memory.app.model.CallState
import com.memory.app.network.ReplayApi
import com.memory.app.network.StartRecordingRequest
import com.memory.app.repository.ContactsRepository
import com.memory.app.repository.SettingsRepository
import com.memory.app.telecom.CallStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class InCallViewModel @Inject constructor(
    private val callStateManager: CallStateManager,
    private val contactsRepository: ContactsRepository,
    private val settingsRepository: SettingsRepository,
    private val replayApi: ReplayApi
) : ViewModel() {

    val currentCall: StateFlow<CallSession?> = callStateManager.currentCall
    val isMuted: StateFlow<Boolean> = callStateManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = callStateManager.isSpeakerOn

    private val _isKeypadOpen = MutableStateFlow(false)
    val isKeypadOpen: StateFlow<Boolean> = _isKeypadOpen.asStateFlow()

    private val _resolvedContactName = MutableStateFlow<String?>(null)

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            currentCall.collect { session ->
                if (session == null) {
                    _resolvedContactName.value = null
                    _durationSeconds.value = 0L
                    _isKeypadOpen.value = false
                } else {
                    if (!session.contactName.isNullOrBlank()) {
                        _resolvedContactName.value = session.contactName
                    } else {
                        resolveContactName(session.phoneNumber)
                    }
                }
            }
        }

        // Ticker for call duration when active
        viewModelScope.launch {
            while (true) {
                val session = currentCall.value
                if (session?.state == CallState.ACTIVE) {
                    val activeStart = session.activeStartedAt ?: session.startedAt
                    val elapsedMillis = System.currentTimeMillis() - activeStart
                    _durationSeconds.value = (elapsedMillis / 1000).coerceAtLeast(0L)
                } else {
                    _durationSeconds.value = 0L
                }
                delay(1000L)
            }
        }
    }

    val formattedDuration: StateFlow<String> = combine(
        currentCall,
        _durationSeconds
    ) { session, seconds ->
        when (session?.state) {
            CallState.DIALING -> "Dialing..."
            CallState.RINGING -> "Incoming Call..."
            CallState.ACTIVE -> formatDuration(seconds)
            CallState.DISCONNECTED -> "Call Ended"
            else -> ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val displayContactName: StateFlow<String> = combine(
        currentCall,
        _resolvedContactName
    ) { session, resolvedName ->
        when {
            !resolvedName.isNullOrBlank() -> resolvedName
            !session?.contactName.isNullOrBlank() -> session?.contactName ?: ""
            !session?.phoneNumber.isNullOrBlank() -> session.phoneNumber
            else -> "Unknown"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Unknown")

    val displayPhoneNumber: StateFlow<String> = combine(
        currentCall,
        _resolvedContactName
    ) { session, resolvedName ->
        if (session != null && !resolvedName.isNullOrBlank() && session.phoneNumber != resolvedName) {
            session.phoneNumber
        } else {
            ""
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    private fun resolveContactName(phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        viewModelScope.launch {
            try {
                val contacts = contactsRepository.getContacts()
                val matched = contacts.find { contact ->
                    contact.phoneNumber.replace("\\s|-".toRegex(), "") == phoneNumber.replace("\\s|-".toRegex(), "")
                }
                if (matched != null) {
                    _resolvedContactName.value = matched.name
                }
            } catch (e: Exception) {
                // Ignore contact lookup failure
            }
        }
    }

    fun answerCall(record: Boolean) {
        callStateManager.answerCall()
        
        if (record) {
            val session = currentCall.value ?: return
            val phoneNumber = session.phoneNumber
            val userPhone = settingsRepository.userPhone.value

            if (userPhone.isBlank()) {
                viewModelScope.launch {
                    _uiEvent.emit("Cannot record: Please set your phone number in Settings.")
                }
                return
            }

            viewModelScope.launch {
                try {
                    val request = StartRecordingRequest(
                        userPhone = userPhone,
                        contactPhone = phoneNumber,
                        userId = "user_123"
                    )
                    val response = replayApi.startRecording(request)

                    if (response.isSuccessful && response.body()?.success == true) {
                        _uiEvent.emit("Recording started!")
                    } else {
                        _uiEvent.emit("Twilio API failed. Using device fallback if enabled.")
                    }
                } catch (e: Exception) {
                    _uiEvent.emit("Twilio error: ${e.message}. Using device fallback if enabled.")
                }
            }
        }
    }

    fun rejectCall() {
        callStateManager.rejectCall()
    }

    fun hangUpCall() {
        callStateManager.disconnectCall()
    }

    fun toggleMute() {
        callStateManager.toggleMute()
    }

    fun toggleSpeaker() {
        callStateManager.toggleSpeaker()
    }

    fun toggleKeypad() {
        _isKeypadOpen.value = !_isKeypadOpen.value
    }

    fun closeKeypad() {
        _isKeypadOpen.value = false
    }

    fun onDtmfKeyPress(digit: Char) {
        callStateManager.playDtmfTone(digit)
    }

    fun onDtmfKeyRelease() {
        callStateManager.stopDtmfTone()
    }

    private fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)
    }
}
