package com.memory.app.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AcousticRecordingProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    companion object {
        private const val TAG = "AcousticRecording"
    }

    @Suppress("DEPRECATION")
    suspend fun startRecording(callId: String): File? = withContext(Dispatchers.IO) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return@withContext null
        }

        try {
            val fileName = "CallRecord_${callId}_${System.currentTimeMillis()}.m4a"
            val file = File(context.filesDir, fileName)
            currentOutputFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "Started acoustic recording to ${file.absolutePath}")
            return@withContext file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start acoustic recording", e)
            cleanup()
            return@withContext null
        }
    }

    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        if (!isRecording) return@withContext null

        try {
            recorder?.apply {
                stop()
                release()
            }
            Log.d(TAG, "Stopped acoustic recording")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        } finally {
            recorder = null
            isRecording = false
        }
        
        return@withContext currentOutputFile
    }

    private fun cleanup() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        recorder = null
        isRecording = false
    }
}
