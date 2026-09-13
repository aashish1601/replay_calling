package com.memory.app.recording

/**
 * Reports what recording-related functionality is actually available
 * on the current device at runtime.
 *
 * IMPORTANT: For ordinary third-party applications, [canDirectCaptureCallAudio]
 * should be reported as false. The Android Telecom/InCallService APIs are for
 * managing calls and the in-call UI, NOT a general-purpose raw call-audio
 * recording API. Becoming the default dialer does NOT grant access to raw
 * call audio.
 */
data class RecordingCapability(
    /** Whether the app can directly capture cellular call audio. Almost always false for third-party apps. */
    val canDirectCaptureCallAudio: Boolean = false,
    /** Whether the app can query MediaStore.Audio for audio files. */
    val canReadMediaStoreAudio: Boolean = false,
    /** Whether Android AudioPlaybackCapture API is available (Android 10+, for non-call audio only). */
    val canUseAudioPlaybackCapture: Boolean = false,
    /** Whether any existing recording files were found on the device. */
    val hasExternalRecordingFiles: Boolean = false,
    /** Paths/locations where recordings were detected (e.g., "Recordings/Call", "PhoneRecord"). */
    val detectedRecordingLocations: List<String> = emptyList(),
    /** Diagnostic notes explaining the capabilities. */
    val notes: List<String> = emptyList()
)
