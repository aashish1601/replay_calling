package com.memory.app.recording

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.memory.app.db.CallDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that scans for newly-available call recordings,
 * imports them, and attempts to match them against recent calls.
 *
 * This worker is scheduled:
 *   1. When a call ends (from MemoryInCallService)
 *   2. Periodically as a one-time delayed work (to catch late recordings)
 *
 * It does NOT continuously run a background service, loop infinitely,
 * or drain battery.
 */
@HiltWorker
class RecordingDiscoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingScanner: RecordingScanner,
    private val recordingRepository: RecordingRepository,
    private val recordingMatcher: RecordingMatcher,
    private val callDao: CallDao
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RecordingDiscovery"
        const val UNIQUE_WORK_NAME = "recording_discovery"

        /**
         * Schedule discovery to run after a short delay,
         * giving the OEM recorder time to flush the file.
         */
        fun schedule(context: Context, delaySeconds: Long = 15) {
            val work = OneTimeWorkRequestBuilder<RecordingDiscoveryWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                work
            )
            Log.d(TAG, "Scheduled recording discovery in ${delaySeconds}s")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting recording discovery scan…")

        return try {
            // 1. Scan MediaStore for potential call recordings
            val discovered = recordingScanner.scanAll()
            Log.d(TAG, "Discovered ${discovered.size} potential recordings")

            var newCount = 0
            var matchedCount = 0

            for (recording in discovered) {
                // 2. Import if not already in database
                val existing = recordingRepository.getByUri(recording.uri.toString())
                if (existing != null) continue  // Already known

                val recordingId = recordingRepository.importDiscovered(recording)
                newCount++

                // 3. Attempt matching against recent calls (last 24 hours)
                val recentCalls = callDao.getRecentCalls(
                    since = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                )

                val entity = recordingRepository.getById(recordingId) ?: continue
                val bestMatch = recordingMatcher.findBestMatch(entity, recentCalls)

                if (bestMatch != null && bestMatch.confidence >= RecordingMatcher.AUTO_MATCH_THRESHOLD) {
                    // Auto-associate
                    recordingRepository.update(
                        entity.copy(
                            associatedCallId = bestMatch.callId,
                            matchConfidence = bestMatch.confidence,
                            processingStatus = "PROCESSED"
                        )
                    )
                    matchedCount++
                    Log.d(TAG, "Auto-matched recording '${recording.displayName}' " +
                            "to call ${bestMatch.callId} (confidence=${bestMatch.confidence})")
                } else if (bestMatch != null && bestMatch.confidence >= RecordingMatcher.SUGGEST_THRESHOLD) {
                    // Store suggestion but don't auto-associate
                    recordingRepository.update(
                        entity.copy(
                            matchConfidence = bestMatch.confidence,
                            processingStatus = "IMPORTED"
                        )
                    )
                    Log.d(TAG, "Suggested match for '${recording.displayName}' " +
                            "to call ${bestMatch.callId} (confidence=${bestMatch.confidence})")
                }
            }

            Log.d(TAG, "Discovery complete: $newCount new, $matchedCount auto-matched")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Discovery failed", e)
            Result.retry()
        }
    }
}
