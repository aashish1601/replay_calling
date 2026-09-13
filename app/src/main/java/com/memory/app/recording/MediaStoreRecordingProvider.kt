package com.memory.app.recording

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import javax.inject.Inject

/**
 * Discovers audio recordings through the standard Android MediaStore.
 *
 * This implementation queries [MediaStore.Audio.Media] and heuristically
 * identifies files that are likely call recordings based on their relative
 * path and filename.
 *
 * It does NOT hardcode a single OEM folder — it scans all audio and
 * scores potential matches against known call-recording patterns.
 */
class MediaStoreRecordingProvider @Inject constructor(
    private val contentResolver: ContentResolver
) : RecordingSourceProvider {

    companion object {
        /**
         * Path fragments commonly associated with call recordings across
         * various OEMs and recorder apps.  We do NOT assume any single one;
         * we use them as heuristic hints.
         */
        private val CALL_RECORDING_PATH_HINTS = listOf(
            "call", "Call", "PhoneRecord", "phonerecord",
            "Recorder", "recorder", "Recording", "recording",
            "CallRecording", "CallRecord", "Calls",
            "voice_call", "VoiceCall", "call_recording",
            "Record", "Audio", "Sounds"
        )
    }

    override suspend fun discoverRecordings(): List<DiscoveredRecording> {
        val recordings = mutableListOf<DiscoveredRecording>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        // RELATIVE_PATH is only available on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        // Only look at audio that is at least 5 seconds long and non-music to
        // reduce false positives.  We also filter out ringtones, notifications, etc.
        val selection = StringBuilder()
        val selectionArgs = mutableListOf<String>()

        selection.append("${MediaStore.Audio.Media.DURATION} > ?")
        selectionArgs.add("5000") // > 5 seconds

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection.toString(),
                selectionArgs.toTypedArray(),
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: "unknown"
                    val mimeType = cursor.getString(mimeCol)
                    val size = cursor.getLong(sizeCol)
                    val duration = cursor.getLong(durationCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000 // seconds → millis
                    val dateMod = cursor.getLong(dateModCol) * 1000
                    val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null

                    // Heuristic: is this likely a call recording?
                    if (!looksLikeCallRecording(displayName, relativePath)) continue

                    val contentUri = Uri.withAppendedPath(collection, id.toString())
                    val source = guessSource(relativePath, displayName)

                    recordings.add(
                        DiscoveredRecording(
                            uri = contentUri,
                            displayName = displayName,
                            mimeType = mimeType,
                            sizeBytes = size,
                            durationMs = duration,
                            dateCreated = dateAdded,
                            dateModified = dateMod,
                            relativePath = relativePath,
                            source = source
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted — caller should handle this.
        }

        return recordings
    }

    /**
     * Heuristic check: does the file name or relative path suggest
     * that this audio file is a call recording?
     */
    private fun looksLikeCallRecording(displayName: String, relativePath: String?): Boolean {
        val nameLower = displayName.lowercase()
        val pathLower = relativePath?.lowercase() ?: ""

        // Check against known path hints
        for (hint in CALL_RECORDING_PATH_HINTS) {
            if (pathLower.contains(hint.lowercase())) return true
        }

        // Common call recording filename patterns
        if (nameLower.contains("call")) return true
        if (nameLower.contains("record")) return true
        if (nameLower.contains("phone")) return true
        if (nameLower.contains("voice_call")) return true

        // Filename looks like a phone number (starts with + or has 7+ digits)
        if (nameLower.matches(Regex(".*\\+?\\d{7,}.*"))) return true

        return false
    }

    /**
     * Try to guess which app / OEM recorder produced this file
     * based on the relative path.
     */
    private fun guessSource(relativePath: String?, displayName: String): RecordingSource {
        val path = relativePath?.lowercase() ?: ""
        val name = displayName.lowercase()

        return when {
            path.contains("com.google.android.dialer") ||
                    path.contains("google") && path.contains("call") -> RecordingSource.GOOGLE_PHONE

            path.contains("phonerecord") || path.contains("oppo") ||
                    path.contains("coloros") -> RecordingSource.OEM_RECORDER

            path.contains("samsung") || path.contains("s recorder") -> RecordingSource.OEM_RECORDER

            path.contains("miui") || path.contains("xiaomi") ||
                    path.contains("sound_recorder") -> RecordingSource.OEM_RECORDER

            path.contains("callrecording") || path.contains("call_recording") ||
                    path.contains("call recording") -> RecordingSource.OEM_RECORDER

            path.contains("recordings/call") || path.contains("record/call") -> RecordingSource.OEM_RECORDER

            path.contains("recorder") || name.contains("record") -> RecordingSource.SYSTEM_RECORDER

            else -> RecordingSource.UNKNOWN
        }
    }
}
