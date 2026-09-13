package com.memory.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecording(recording: RecordingEntity): Long

    @Update
    suspend fun updateRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecording(id: Long)

    @Query("SELECT * FROM recordings ORDER BY importedAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY importedAt DESC")
    fun observeRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE contentUri = :uri LIMIT 1")
    suspend fun getRecordingByUri(uri: String): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE associatedCallId = :callId")
    suspend fun getRecordingsForCall(callId: Long): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE associatedCallId = :callId")
    fun observeRecordingsForCall(callId: Long): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Query("SELECT COUNT(*) FROM recordings")
    suspend fun getRecordingCount(): Int

    @Query("SELECT * FROM recordings WHERE associatedCallId IS NULL ORDER BY importedAt DESC")
    fun observeUnassociatedRecordings(): Flow<List<RecordingEntity>>
}
