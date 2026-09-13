package com.memory.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CallUtilsTest {

    @Test
    fun formatDuration_zeroOrNull_returnsZeroSeconds() {
        assertEquals("0s", CallUtils.formatDuration(null))
        assertEquals("0s", CallUtils.formatDuration(0L))
    }

    @Test
    fun formatDuration_underOneMinute_returnsSeconds() {
        assertEquals("45s", CallUtils.formatDuration(45000L))
    }

    @Test
    fun formatDuration_overOneMinute_returnsMinutesAndSeconds() {
        assertEquals("2m 15s", CallUtils.formatDuration(135000L))
    }
}
