package com.memory.app.db

/**
 * Tracks the transcription lifecycle of a recording.
 * Actual transcription is Phase 3; this enum reserves the state.
 */
enum class TranscriptStatus {
    NOT_STARTED,
    PROCESSING,
    COMPLETED,
    FAILED
}
