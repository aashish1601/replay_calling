package com.memory.app.repository

import android.content.Context
import android.content.SharedPreferences
import com.memory.app.model.RecordingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("memory_settings", Context.MODE_PRIVATE)
    
    private val _recordingMode = MutableStateFlow(getSavedRecordingMode())
    val recordingMode: StateFlow<RecordingMode> = _recordingMode.asStateFlow()

    private val _userPhone = MutableStateFlow(getSavedUserPhone())
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    fun setRecordingMode(mode: RecordingMode) {
        prefs.edit().putString(KEY_RECORDING_MODE, mode.name).apply()
        _recordingMode.value = mode
    }

    fun setUserPhone(phone: String) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply()
        _userPhone.value = phone
    }

    private fun getSavedRecordingMode(): RecordingMode {
        val saved = prefs.getString(KEY_RECORDING_MODE, RecordingMode.NATIVE.name)
        return try {
            RecordingMode.valueOf(saved ?: RecordingMode.NATIVE.name)
        } catch (e: Exception) {
            RecordingMode.NATIVE
        }
    }

    private fun getSavedUserPhone(): String {
        return prefs.getString(KEY_USER_PHONE, "") ?: ""
    }

    companion object {
        private const val KEY_RECORDING_MODE = "recording_mode"
        private const val KEY_USER_PHONE = "user_phone"
    }
}
