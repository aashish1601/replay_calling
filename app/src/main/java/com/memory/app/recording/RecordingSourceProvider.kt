package com.memory.app.recording

/**
 * Abstraction for discovering audio recordings from a specific source.
 * Each implementation handles one category of recording origin.
 */
interface RecordingSourceProvider {
    /** Scan the source and return all discovered recordings. */
    suspend fun discoverRecordings(): List<DiscoveredRecording>
}
