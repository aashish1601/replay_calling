package com.memory.app.recording

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates all [RecordingSourceProvider] implementations and
 * merges their results into a single de-duplicated list.
 */
@Singleton
class RecordingScanner @Inject constructor(
    private val mediaStoreProvider: MediaStoreRecordingProvider
    // Future providers (OppoRecordingProvider, GooglePhoneRecordingProvider, etc.)
    // can be added here via constructor injection.
) {
    /**
     * Run all registered providers and return de-duplicated results.
     */
    suspend fun scanAll(): List<DiscoveredRecording> {
        val all = mutableListOf<DiscoveredRecording>()

        // MediaStore is the primary provider for now
        all.addAll(mediaStoreProvider.discoverRecordings())

        // De-duplicate by URI
        return all.distinctBy { it.uri.toString() }
    }
}
