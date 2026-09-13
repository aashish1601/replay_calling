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
open class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences? = try {
        context.getSharedPreferences("memory_settings", Context.MODE_PRIVATE)
    } catch (e: Exception) {
        null
    }
    
    private val _recordingMode = MutableStateFlow(getSavedRecordingMode())
    open val recordingMode: StateFlow<RecordingMode> = _recordingMode.asStateFlow()

    private val _userPhone = MutableStateFlow(getSavedUserPhone())
    open val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _twilioPhone = MutableStateFlow(getSavedTwilioPhone())
    open val twilioPhone: StateFlow<String> = _twilioPhone.asStateFlow()

    open fun setRecordingMode(mode: RecordingMode) {
        prefs?.edit()?.putString(KEY_RECORDING_MODE, mode.name)?.apply()
        _recordingMode.value = mode
    }

    open fun setUserPhone(phone: String) {
        prefs?.edit()?.putString(KEY_USER_PHONE, phone)?.apply()
        _userPhone.value = phone
    }

    open fun setTwilioPhone(phone: String) {
        prefs?.edit()?.putString(KEY_TWILIO_PHONE, phone)?.apply()
        _twilioPhone.value = phone
    }

    private fun getSavedRecordingMode(): RecordingMode {
        val saved = prefs?.getString(KEY_RECORDING_MODE, RecordingMode.NATIVE.name)
        return try {
            RecordingMode.valueOf(saved ?: RecordingMode.NATIVE.name)
        } catch (e: Exception) {
            RecordingMode.NATIVE
        }
    }

    private fun getSavedUserPhone(): String {
        return prefs?.getString(KEY_USER_PHONE, "") ?: ""
    }

    private fun getSavedTwilioPhone(): String {
        return prefs?.getString(KEY_TWILIO_PHONE, "") ?: ""
    }

    companion object {
        private const val KEY_RECORDING_MODE = "recording_mode"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_TWILIO_PHONE = "twilio_phone"
    }
}
