package com.memory.app.recording

/**
 * Identifies the source/origin of a discovered call recording.
 */
enum class RecordingSource {
    /** Recording made by the device OEM's built-in call recorder (e.g., OPPO, Samsung, Xiaomi). */
    OEM_RECORDER,
    /** Recording made by Google's Phone (Dialer) app. */
    GOOGLE_PHONE,
    /** Recording made by the system's built-in sound recorder. */
    SYSTEM_RECORDER,
    /** Recording manually imported by the user via Storage Access Framework. */
    USER_IMPORTED,
    /** Origin could not be determined. */
    UNKNOWN
}
