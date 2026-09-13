package com.memory.app.recording

import com.memory.app.db.CallEntity
import com.memory.app.db.RecordingEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

/**
 * Deterministic matching algorithm that scores how likely a
 * [RecordingEntity] belongs to a given [CallEntity].
 *
 * Scoring breakdown (max 100 points):
 *   Time overlap / proximity :  0–40 points
 *   Duration similarity      :  0–25 points
 *   Phone number in filename :  0–20 points
 *   Contact name in filename :  0–10 points
 *   Folder / source evidence :  0– 5 points
 *
 * Thresholds:
 *   >= 0.80  →  auto-associate
 *   0.50–0.79 →  suggest to user ("Possible call recording")
 *   < 0.50  →  do not associate
 */
@Singleton
class RecordingMatcher @Inject constructor() {

    companion object {
        const val AUTO_MATCH_THRESHOLD = 0.80f
        const val SUGGEST_THRESHOLD = 0.50f

        // Weight caps (must sum to 100)
        private const val MAX_TIME_SCORE = 40
        private const val MAX_DURATION_SCORE = 25
        private const val MAX_PHONE_SCORE = 20
        private const val MAX_NAME_SCORE = 10
        private const val MAX_FOLDER_SCORE = 5
    }

    /**
     * Find the best match for [recording] among the given [calls].
     * Returns null if no call reaches the [SUGGEST_THRESHOLD].
     */
    fun findBestMatch(
        recording: RecordingEntity,
        calls: List<CallEntity>
    ): RecordingMatch? {
        if (calls.isEmpty()) return null
        return calls
            .map { call -> score(recording, call) }
            .filter { it.confidence >= SUGGEST_THRESHOLD }
            .maxByOrNull { it.confidence }
    }

    /**
     * Score a single recording against a single call.
     */
    fun score(recording: RecordingEntity, call: CallEntity): RecordingMatch {
        var totalPoints = 0
        val reasons = mutableListOf<String>()

        // ── 1. Time proximity (max 40 pts) ────────────────────────
        totalPoints += scoreTimeProximity(recording, call, reasons)

        // ── 2. Duration similarity (max 25 pts) ───────────────────
        totalPoints += scoreDuration(recording, call, reasons)

        // ── 3. Phone number in filename (max 20 pts) ──────────────
        totalPoints += scorePhoneNumber(recording, call, reasons)

        // ── 4. Contact name in filename (max 10 pts) ──────────────
        totalPoints += scoreContactName(recording, call, reasons)

        // ── 5. Folder / source evidence (max 5 pts) ───────────────
        totalPoints += scoreFolderSource(recording, reasons)

        val confidence = totalPoints / 100f

        return RecordingMatch(
            recordingId = recording.id,
            callId = call.id,
            confidence = confidence.coerceIn(0f, 1f),
            reasons = reasons
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Scoring functions
    // ─────────────────────────────────────────────────────────────────

    private fun scoreTimeProximity(
        recording: RecordingEntity,
        call: CallEntity,
        reasons: MutableList<String>
    ): Int {
        val recTime = recording.dateCreated ?: recording.dateModified ?: return 0
        val callStart = call.startedAt
        val callEnd = call.endedAt ?: (callStart + (call.duration ?: 0L))

        // Case 1: Recording creation falls inside the call window
        if (recTime in callStart..callEnd) {
            reasons.add("Recording creation time falls within the call window")
            return MAX_TIME_SCORE
        }

        // Case 2: Close proximity — within a few minutes
        val distToStart = abs(recTime - callStart)
        val distToEnd = abs(recTime - callEnd)
        val closestDistance = min(distToStart, distToEnd)

        return when {
            closestDistance <= 30_000 -> {     // ≤ 30 seconds
                reasons.add("Recording timestamp is within 30s of the call")
                MAX_TIME_SCORE
            }
            closestDistance <= 60_000 -> {     // ≤ 1 minute
                reasons.add("Recording timestamp is within 1 min of the call")
                35
            }
            closestDistance <= 120_000 -> {    // ≤ 2 minutes
                reasons.add("Recording timestamp is within 2 min of the call")
                28
            }
            closestDistance <= 300_000 -> {    // ≤ 5 minutes
                reasons.add("Recording timestamp is within 5 min of the call")
                18
            }
            closestDistance <= 600_000 -> {    // ≤ 10 minutes
                reasons.add("Recording timestamp is within 10 min of the call")
                8
            }
            else -> {
                0
            }
        }
    }

    private fun scoreDuration(
        recording: RecordingEntity,
        call: CallEntity,
        reasons: MutableList<String>
    ): Int {
        val recDuration = recording.durationMs ?: return 0
        val callDuration = call.duration ?: return 0

        if (callDuration <= 0 || recDuration <= 0) return 0

        val diff = abs(recDuration - callDuration)
        val maxDur = maxOf(recDuration, callDuration).toFloat()
        val ratio = 1f - (diff / maxDur)

        return when {
            ratio >= 0.95f -> {
                reasons.add("Duration is nearly identical (${formatMs(recDuration)} vs ${formatMs(callDuration)})")
                MAX_DURATION_SCORE
            }
            ratio >= 0.85f -> {
                reasons.add("Duration is very close (${formatMs(recDuration)} vs ${formatMs(callDuration)})")
                20
            }
            ratio >= 0.70f -> {
                reasons.add("Duration is somewhat close (${formatMs(recDuration)} vs ${formatMs(callDuration)})")
                14
            }
            ratio >= 0.50f -> {
                reasons.add("Duration partially matches (${formatMs(recDuration)} vs ${formatMs(callDuration)})")
                7
            }
            else -> 0
        }
    }

    private fun scorePhoneNumber(
        recording: RecordingEntity,
        call: CallEntity,
        reasons: MutableList<String>
    ): Int {
        val filename = recording.displayName.lowercase()
        val phone = call.phoneNumber.replace(Regex("[^0-9+]"), "")

        if (phone.length < 4) return 0

        // Try full number
        if (filename.contains(phone.replace("+", ""))) {
            reasons.add("Phone number found in filename")
            return MAX_PHONE_SCORE
        }

        // Try last 7+ digits
        val last7 = phone.takeLast(7)
        if (last7.length >= 7 && filename.contains(last7)) {
            reasons.add("Last 7 digits of phone number found in filename")
            return 15
        }

        // Try last 4 digits
        val last4 = phone.takeLast(4)
        if (last4.length >= 4 && filename.contains(last4)) {
            reasons.add("Last 4 digits of phone number found in filename")
            return 8
        }

        return 0
    }

    private fun scoreContactName(
        recording: RecordingEntity,
        call: CallEntity,
        reasons: MutableList<String>
    ): Int {
        val name = call.contactName?.trim() ?: return 0
        if (name.isBlank() || name.length < 2) return 0

        val filename = recording.displayName.lowercase()
        val nameLower = name.lowercase()

        return when {
            filename.contains(nameLower) -> {
                reasons.add("Contact name '$name' found in filename")
                MAX_NAME_SCORE
            }
            // Try first name only
            nameLower.contains(" ") && filename.contains(nameLower.substringBefore(" ")) -> {
                reasons.add("First name '${name.substringBefore(" ")}' found in filename")
                6
            }
            else -> 0
        }
    }

    private fun scoreFolderSource(
        recording: RecordingEntity,
        reasons: MutableList<String>
    ): Int {
        val path = recording.relativePath?.lowercase() ?: ""
        val source = recording.source

        return when {
            source == "OEM_RECORDER" || source == "GOOGLE_PHONE" -> {
                reasons.add("Recording source identified as call recorder ($source)")
                MAX_FOLDER_SCORE
            }
            path.contains("call") -> {
                reasons.add("Recording is in a 'call' folder")
                MAX_FOLDER_SCORE
            }
            path.contains("phonerecord") || path.contains("voice") -> {
                reasons.add("Recording is in a voice/phone record folder")
                3
            }
            else -> 0
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "${min}m ${sec}s"
    }
}
