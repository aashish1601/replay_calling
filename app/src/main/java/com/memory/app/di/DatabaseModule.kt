package com.memory.app.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.memory.app.db.CallDao
import com.memory.app.db.MemoryDatabase
import com.memory.app.db.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMemoryDatabase(@ApplicationContext context: Context): MemoryDatabase {
        return Room.databaseBuilder(
            context,
            MemoryDatabase::class.java,
            "memory_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCallDao(database: MemoryDatabase): CallDao {
        return database.callDao()
    }

    @Provides
    fun provideRecordingDao(database: MemoryDatabase): RecordingDao {
        return database.recordingDao()
    }

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
}

