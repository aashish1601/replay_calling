package com.memory.app.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Insert
    suspend fun insert(call: CallEntity): Long

    @Query("SELECT * FROM calls ORDER BY startedAt DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Query("SELECT * FROM calls WHERE id = :id")
    suspend fun getCallById(id: Long): CallEntity?

    @Query("SELECT * FROM calls WHERE startedAt >= :since ORDER BY startedAt DESC")
    suspend fun getRecentCalls(since: Long): List<CallEntity>
}
