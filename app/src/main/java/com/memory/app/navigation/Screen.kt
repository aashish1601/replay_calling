package com.memory.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
    @Serializable
    data object Recents : Screen

    @Serializable
    data object Contacts : Screen

    @Serializable
    data object Keypad : Screen

    @Serializable
    data object Memory : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data class CallDetail(val callId: Long) : Screen

    @Serializable
    data class RecordingDetail(val recordingId: Long) : Screen

    @Serializable
    data object RecordingDiagnostics : Screen

    @Serializable
    data object ManualImport : Screen
}
