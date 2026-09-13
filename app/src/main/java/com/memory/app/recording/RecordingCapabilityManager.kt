package com.memory.app.recording

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines what recording-related functionality is actually available
 * on the current device at runtime.
 *
 * IMPORTANT:
 * For ordinary third-party applications, direct cellular call-audio
 * capture is reported as UNAVAILABLE.
 *
 * Becoming the default dialer does NOT grant raw call-audio access.
 * The Android Telecom / InCallService APIs manage calls and the
 * in-call UI; they are NOT a general-purpose recording API.
 */
@Singleton
class RecordingCapabilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Build a snapshot of the device's recording capabilities. */
    fun detectCapabilities(): RecordingCapability {
        val notes = mutableListOf<String>()
        val detectedLocations = mutableListOf<String>()

        // ── Direct call-audio capture ──────────────────────────────
        // Always false for third-party apps on stock Android.
        val canDirectCapture = false
        notes.add(
            "Direct cellular call-audio capture is NOT AVAILABLE " +
                    "through ordinary public API for third-party apps."
        )

        // ── MediaStore audio access ────────────────────────────────
        val canReadMediaStore = canReadAudioFromMediaStore()
        if (canReadMediaStore) {
            notes.add("MediaStore.Audio access is available.")
        } else {
            notes.add(
                "MediaStore.Audio access requires permission. " +
                        "Grant READ_MEDIA_AUDIO (Android 13+) or READ_EXTERNAL_STORAGE."
            )
        }

        // ── Audio Playback Capture (Android 10+, non-call only) ───
        val canPlaybackCapture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (canPlaybackCapture) {
            notes.add(
                "AudioPlaybackCapture API exists (Android 10+) but does NOT " +
                        "capture telephony audio for third-party apps."
            )
        }

        // ── OEM / device detection ─────────────────────────────────
        val manufacturer = Build.MANUFACTURER.uppercase()
        val model = Build.MODEL
        val brand = Build.BRAND.uppercase()

        when {
            manufacturer.contains("OPPO") || brand.contains("OPPO") || brand.contains("REALME") -> {
                notes.add("OPPO/Realme device detected: $manufacturer $model.")
                notes.add(
                    "OPPO devices may store call recordings via the OEM Phone app. " +
                            "Use MediaStore or manual import to access them."
                )
            }
            manufacturer.contains("SAMSUNG") -> {
                notes.add("Samsung device detected: $manufacturer $model.")
            }
            manufacturer.contains("XIAOMI") || brand.contains("REDMI") || brand.contains("POCO") -> {
                notes.add("Xiaomi/Redmi/POCO device detected: $manufacturer $model.")
            }
            manufacturer.contains("GOOGLE") -> {
                notes.add("Google device detected: $manufacturer $model.")
                notes.add("Google Phone app may store recordings in the Recordings folder.")
            }
            else -> {
                notes.add("Device: $manufacturer $model.")
            }
        }

        return RecordingCapability(
            canDirectCaptureCallAudio = canDirectCapture,
            canReadMediaStoreAudio = canReadMediaStore,
            canUseAudioPlaybackCapture = canPlaybackCapture,
            hasExternalRecordingFiles = false, // Updated after first scan
            detectedRecordingLocations = detectedLocations,
            notes = notes
        )
    }

    /** Quick device-info summary for the diagnostics screen. */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT
        )
    }

    private fun canReadAudioFromMediaStore(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val androidVersion: String,
    val sdkVersion: Int
)
