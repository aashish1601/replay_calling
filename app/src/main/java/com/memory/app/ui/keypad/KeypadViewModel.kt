package com.memory.app.ui.keypad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memory.app.network.ReplayApi
import com.memory.app.network.StartRecordingRequest
import com.memory.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class KeypadUiEvent {
    data class ShowToast(val message: String) : KeypadUiEvent()
    data class LaunchNativeCall(val phoneNumber: String) : KeypadUiEvent()
}

@HiltViewModel
class KeypadViewModel @Inject constructor(
    private val replayApi: ReplayApi,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _uiEvent = MutableSharedFlow<KeypadUiEvent>()
    val uiEvent: SharedFlow<KeypadUiEvent> = _uiEvent.asSharedFlow()

    fun appendDigit(digit: String) {
        _phoneNumber.value += digit
    }

    fun deleteLastDigit() {
        if (_phoneNumber.value.isNotEmpty()) {
            _phoneNumber.value = _phoneNumber.value.dropLast(1)
        }
    }

    fun clear() {
        _phoneNumber.value = ""
    }

    fun startCall(phoneNumber: String) {
        viewModelScope.launch {
            _uiEvent.emit(KeypadUiEvent.LaunchNativeCall(phoneNumber))
        }
    }
}
