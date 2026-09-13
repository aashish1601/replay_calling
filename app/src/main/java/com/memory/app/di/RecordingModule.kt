package com.memory.app.di

import com.memory.app.recording.MediaStoreRecordingProvider
import com.memory.app.recording.RecordingSourceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {
    // If we add more providers in the future, we could use @IntoSet,
    // but for now MediaStore is our primary/only automated provider.
}
