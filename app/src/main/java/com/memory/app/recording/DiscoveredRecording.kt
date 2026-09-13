package com.memory.app.recording

import android.net.Uri

/**
 * Represents an audio recording file discovered on the device.
 *
 * We store the content URI as the source reference; we do NOT
 * eagerly copy every file into the app's private storage.
 */
data class DiscoveredRecording(
    /** Content URI pointing to the audio file (MediaStore or SAF-persisted URI). */
    val uri: Uri,
    /** User-visible file name. */
    val displayName: String,
    /** MIME type, e.g. "audio/mp4", "audio/mpeg". */
    val mimeType: String? = null,
    /** Size in bytes. */
    val sizeBytes: Long? = null,
    /** Duration in milliseconds. */
    val durationMs: Long? = null,
    /** Timestamp when the file was originally created (epoch millis). */
    val dateCreated: Long? = null,
    /** Timestamp when the file was last modified (epoch millis). */
    val dateModified: Long? = null,
    /** Relative path inside external storage (e.g., "Recordings/Call"). */
    val relativePath: String? = null,
    /** Best-guess source of this recording. */
    val source: RecordingSource = RecordingSource.UNKNOWN
)
