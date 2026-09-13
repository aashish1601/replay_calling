package com.memory.app.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CallEntity::class, RecordingEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun recordingDao(): RecordingDao
}

