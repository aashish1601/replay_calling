package com.memory.app.recording

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.memory.app.db.CallEntity
import com.memory.app.model.RecordingMode
import com.memory.app.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val acousticProvider: AcousticRecordingProvider,
    private val recordingRepository: RecordingRepository
) {
    private val engineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "RecordingEngine"
    }

    /**
     * Called by the InCallService when a call becomes ACTIVE.
     * Returns true if the acoustic provider started, so the caller knows to force speakerphone.
     */
    suspend fun onCallActive(callId: String): Boolean {
        val mode = settingsRepository.recordingMode.value
        Log.d(TAG, "onCallActive for $callId. Mode is $mode")
        
        return if (mode == RecordingMode.ACOUSTIC) {
            val file = acousticProvider.startRecording(callId)
            file != null
        } else {
            false
        }
    }

    /**
     * Called by the InCallService when a call is DISCONNECTED.
     */
    fun onCallDisconnected(call: CallEntity) {
        val mode = settingsRepository.recordingMode.value
        Log.d(TAG, "onCallDisconnected for ${call.id}. Mode is $mode")

        engineScope.launch {
            when (mode) {
                RecordingMode.ACOUSTIC -> {
                    // Stop acoustic recording and immediately associate
                    val file = acousticProvider.stopRecording()
                    if (file != null && file.exists() && file.length() > 0) {
                        val discovered = DiscoveredRecording(
                            uri = file.toUri(),
                            displayName = file.name,
                            mimeType = "audio/mp4",
                            sizeBytes = file.length(),
                            dateCreated = System.currentTimeMillis(),
                            dateModified = System.currentTimeMillis(),
                            relativePath = null,
                            source = RecordingSource.SYSTEM_RECORDER
                        )
                        val recordingId = recordingRepository.importDiscovered(discovered)
                        
                        // Fetch again and associate
                        val entity = recordingRepository.getById(recordingId)
                        if (entity != null) {
                            recordingRepository.update(
                                entity.copy(
                                    associatedCallId = call.id,
                                    matchConfidence = 1.0f,
                                    processingStatus = "PROCESSED"
                                )
                            )
                        }
                    } else {
                        Log.w(TAG, "Acoustic recording resulted in empty/null file.")
                    }
                }
                RecordingMode.NATIVE -> {
                    // Schedule background discovery to scan MediaStore
                    RecordingDiscoveryWorker.schedule(context, delaySeconds = 15)
                }
                RecordingMode.ASK_ME -> {
                    // Could trigger a notification here. For now, we do nothing
                    // and rely on the user to go to the Memory tab to manually import.
                    Log.d(TAG, "ASK_ME mode: awaiting user action.")
                }
                RecordingMode.OFF -> {
                    Log.d(TAG, "Recording mode is OFF.")
                }
            }
        }
    }
}
