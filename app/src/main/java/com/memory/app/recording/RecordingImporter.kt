package com.memory.app.recording

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.memory.app.db.RecordingEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles importing recordings from user-selected URIs
 * (Storage Access Framework / ACTION_OPEN_DOCUMENT).
 */
@Singleton
class RecordingImporter @Inject constructor(
    private val contentResolver: ContentResolver,
    private val recordingRepository: RecordingRepository
) {
    /**
     * Import a recording from a user-selected SAF URI.
     * Reads metadata from the content resolver and persists to the database.
     * Returns the database ID of the newly created recording.
     */
    suspend fun importFromUri(uri: Uri): Long {
        // Check if already imported
        val existing = recordingRepository.getByUri(uri.toString())
        if (existing != null) return existing.id

        var displayName = "Imported Recording"
        var size: Long? = null
        var mimeType: String? = null

        // Read metadata from the content resolver
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                    if (sizeIdx >= 0) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {
            // Best-effort metadata read
        }

        mimeType = contentResolver.getType(uri)

        val entity = RecordingEntity(
            contentUri = uri.toString(),
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = size,
            dateCreated = System.currentTimeMillis(),
            dateModified = System.currentTimeMillis(),
            source = RecordingSource.USER_IMPORTED.name,
            importedAt = System.currentTimeMillis(),
            processingStatus = "IMPORTED"
        )

        return recordingRepository.insert(entity)
    }
}
