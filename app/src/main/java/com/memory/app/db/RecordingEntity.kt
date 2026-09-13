package com.memory.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity persisting metadata about a discovered or imported
 * call recording. The actual audio data lives at [contentUri];
 * we do NOT copy raw audio into the database.
 */
@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Content URI string (MediaStore or SAF-persisted). */
    val contentUri: String,
    /** User-visible file name. */
    val displayName: String,
    /** MIME type, e.g. "audio/mp4". */
    val mimeType: String? = null,
    /** File size in bytes. */
    val sizeBytes: Long? = null,
    /** Audio duration in milliseconds. */
    val durationMs: Long? = null,
    /** When the file was originally created (epoch millis). */
    val dateCreated: Long? = null,
    /** When the file was last modified (epoch millis). */
    val dateModified: Long? = null,
    /** Relative path inside external storage. */
    val relativePath: String? = null,
    /** Source that produced this recording. */
    val source: String = "UNKNOWN",
    /** Timestamp when Memory imported / first saw this recording. */
    val importedAt: Long = System.currentTimeMillis(),
    /** FK-like reference to CallEntity.id; null if unassociated. */
    val associatedCallId: Long? = null,
    /** Match confidence when auto-associated (0.0–1.0). */
    val matchConfidence: Float? = null,
    /** Processing status. */
    val processingStatus: String = "DISCOVERED",
    /** Transcription status. */
    val transcriptStatus: String = "NOT_STARTED"
)
