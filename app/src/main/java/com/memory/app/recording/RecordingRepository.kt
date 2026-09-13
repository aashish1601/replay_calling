package com.memory.app.recording

import com.memory.app.db.RecordingDao
import com.memory.app.db.RecordingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for persisted recording data.
 * Wraps [RecordingDao] and provides higher-level operations.
 */
@Singleton
class RecordingRepository @Inject constructor(
    private val recordingDao: RecordingDao
) {
    fun observeAll(): Flow<List<RecordingEntity>> = recordingDao.observeRecordings()

    fun observeForCall(callId: Long): Flow<List<RecordingEntity>> =
        recordingDao.observeRecordingsForCall(callId)

    fun observeUnassociated(): Flow<List<RecordingEntity>> =
        recordingDao.observeUnassociatedRecordings()

    suspend fun getByUri(uri: String): RecordingEntity? =
        recordingDao.getRecordingByUri(uri)

    suspend fun getById(id: Long): RecordingEntity? =
        recordingDao.getRecordingById(id)

    suspend fun getForCall(callId: Long): List<RecordingEntity> =
        recordingDao.getRecordingsForCall(callId)

    suspend fun insert(entity: RecordingEntity): Long =
        recordingDao.insertRecording(entity)

    suspend fun update(entity: RecordingEntity) =
        recordingDao.updateRecording(entity)

    suspend fun delete(id: Long) =
        recordingDao.deleteRecording(id)

    suspend fun getCount(): Int =
        recordingDao.getRecordingCount()

    /**
     * Import a [DiscoveredRecording] into the database, if not already present.
     * Returns the DB id of the entity (existing or newly created).
     */
    suspend fun importDiscovered(recording: DiscoveredRecording): Long {
        val existing = getByUri(recording.uri.toString())
        if (existing != null) return existing.id

        val entity = RecordingEntity(
            contentUri = recording.uri.toString(),
            displayName = recording.displayName,
            mimeType = recording.mimeType,
            sizeBytes = recording.sizeBytes,
            durationMs = recording.durationMs,
            dateCreated = recording.dateCreated,
            dateModified = recording.dateModified,
            relativePath = recording.relativePath,
            source = recording.source.name,
            importedAt = System.currentTimeMillis(),
            processingStatus = "IMPORTED"
        )
        return insert(entity)
    }
}
