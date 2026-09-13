package com.memory.app.recording

/**
 * Represents a scored match between a [com.memory.app.db.RecordingEntity]
 * and a [com.memory.app.db.CallEntity].
 *
 * Confidence is 0.0 – 1.0.
 *   >= 0.80 → auto-associate
 *   0.50 – 0.79 → suggest to user ("Possible call recording")
 *   < 0.50 → do not automatically associate
 */
data class RecordingMatch(
    /** Database ID (or URI string) of the recording. */
    val recordingId: Long,
    /** Database ID of the call. */
    val callId: Long,
    /** Normalised confidence score, 0.0 – 1.0. */
    val confidence: Float,
    /** Human-readable reasons explaining the score. */
    val reasons: List<String>
)
