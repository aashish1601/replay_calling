package com.memory.app.recording

interface RecordingProvider {
    fun startRecording(callId: String)
    fun stopRecording(callId: String)
}
