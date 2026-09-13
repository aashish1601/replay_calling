package com.memory.app.ui.settings

import androidx.lifecycle.ViewModel
import com.memory.app.model.RecordingMode
import com.memory.app.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val recordingMode: StateFlow<RecordingMode> = settingsRepository.recordingMode
    val userPhone: StateFlow<String> = settingsRepository.userPhone

    fun setRecordingMode(mode: RecordingMode) {
        settingsRepository.setRecordingMode(mode)
    }

    fun setUserPhone(phone: String) {
        settingsRepository.setUserPhone(phone)
    }
}
