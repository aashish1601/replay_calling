package com.memory.app.recording

import com.memory.app.db.CallEntity
import com.memory.app.db.RecordingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordingMatcherTest {

    private lateinit var matcher: RecordingMatcher

    @Before
    fun setup() {
        matcher = RecordingMatcher()
    }

    @Test
    fun testExactTimeMatch_scoresHigh() {
        // Call from 10:00:00 to 10:05:00
        val callStart = 1600000000000L
        val callDuration = 300_000L
        val callEnd = callStart + callDuration

        val call = CallEntity(
            id = 1,
            phoneNumber = "+1234567890",
            direction = "INCOMING",
            startedAt = callStart,
            endedAt = callEnd,
            duration = callDuration
        )

        // Recording created at 10:00:15
        val recording = RecordingEntity(
            id = 100,
            contentUri = "content://media/audio/1",
            displayName = "call_recording.mp4",
            durationMs = callDuration,
            dateCreated = callStart + 15_000L,
            source = "OEM_RECORDER"
        )

        val match = matcher.score(recording, call)
        
        // Time inside window (40) + exact duration (25) + OEM_RECORDER source (5)
        // Expected total: 70 points out of 100
        assertTrue("Confidence should be very high", match.confidence >= 0.70f)
        assertTrue("Should contain reason for time match", match.reasons.any { it.contains("within the call window") })
        assertTrue("Should contain reason for duration match", match.reasons.any { it.contains("nearly identical") })
    }

    @Test
    fun testPhoneNumberMatch_scoresHigh() {
        val callStart = 1600000000000L
        val call = CallEntity(
            id = 1,
            phoneNumber = "+1987654321",
            direction = "OUTGOING",
            startedAt = callStart,
            duration = 120_000L
        )

        val recording = RecordingEntity(
            id = 101,
            contentUri = "content://media/audio/2",
            displayName = "Record_1987654321_Aug15.amr", // Full number in filename
            dateCreated = callStart - 1000L, // 1 sec before call
            durationMs = 122_000L // 2 sec longer
        )

        val match = matcher.score(recording, call)

        // Close time (40) + very close duration (20) + full phone number (20)
        // Expected total: 80 points
        assertTrue("Confidence should be >= 0.80 for auto-match", match.confidence >= RecordingMatcher.AUTO_MATCH_THRESHOLD)
        assertTrue(match.reasons.any { it.contains("Phone number found") })
    }
    
    @Test
    fun testContactNameMatch_scoresWell() {
        val callStart = 1600000000000L
        val call = CallEntity(
            id = 1,
            phoneNumber = "+1234567890",
            contactName = "Alice Smith",
            direction = "INCOMING",
            startedAt = callStart,
            duration = 60_000L
        )

        val recording = RecordingEntity(
            id = 102,
            contentUri = "content://media/audio/3",
            displayName = "Call with Alice Smith.m4a",
            dateCreated = callStart + 65_000L, // 5 seconds after call ends
            durationMs = 60_000L
        )

        val match = matcher.score(recording, call)

        assertTrue(match.reasons.any { it.contains("Contact name 'Alice Smith' found") })
        assertTrue("Should reach suggest threshold", match.confidence >= RecordingMatcher.SUGGEST_THRESHOLD)
    }

    @Test
    fun testNoMatch_scoresLow() {
        val callStart = 1600000000000L
        val call = CallEntity(
            id = 1,
            phoneNumber = "+1234567890",
            direction = "INCOMING",
            startedAt = callStart,
            duration = 60_000L
        )

        // Completely unrelated recording hours later
        val recording = RecordingEntity(
            id = 103,
            contentUri = "content://media/audio/4",
            displayName = "voice_memo_1.m4a",
            dateCreated = callStart + 3600_000L, // 1 hour later
            durationMs = 5000L
        )

        val match = matcher.score(recording, call)

        assertTrue("Confidence should be extremely low", match.confidence < 0.20f)
    }
}
