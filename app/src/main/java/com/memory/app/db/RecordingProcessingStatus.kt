package com.memory.app.db

/**
 * Tracks the processing lifecycle of a discovered recording.
 */
enum class RecordingProcessingStatus {
    /** Recording discovered but not yet fully imported / metadata read. */
    DISCOVERED,
    /** Recording imported and metadata persisted. */
    IMPORTED,
    /** Recording is being processed (e.g., preparing for transcription). */
    PROCESSING,
    /** Processing complete and ready for transcription. */
    PROCESSED,
    /** Processing failed. */
    FAILED
}
