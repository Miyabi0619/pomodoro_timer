package com.miyabi0619.pomodorotimer

import com.miyabi0619.pomodorotimer.timer.DurationInput
import com.miyabi0619.pomodorotimer.timer.TimerSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationInputTest {
    private fun type(vararg keys: String): String =
        keys.fold("") { buffer, key -> DurationInput.append(buffer, key) }

    @Test
    fun typingDigits_fillsFromTheRight() {
        val buffer = type("1", "5", "0", "0")

        assertEquals("001500", DurationInput.padded(buffer))
        assertEquals(15 * 60, DurationInput.toSeconds(buffer))
    }

    @Test
    fun leadingZeroKeys_areIgnored() {
        assertEquals("", type("0", "00", "0"))
        assertEquals("100", type("00", "1", "00"))
    }

    @Test
    fun input_stopsAtSixDigits() {
        val buffer = type("1", "2", "3", "4", "5", "6", "7", "00")

        assertEquals("123456", buffer)
    }

    @Test
    fun delete_removesLastDigitAndIsSafeWhenEmpty() {
        assertEquals("15", DurationInput.delete("150"))
        assertEquals("", DurationInput.delete(""))
    }

    @Test
    fun overflowingSeconds_areNormalized() {
        assertEquals(90, DurationInput.toSeconds("90"))
        assertEquals(60 * 60 + 40 * 60 + 40, DurationInput.toSeconds("1" + "40" + "40"))
    }

    @Test
    fun tooLargeValue_isClampedToMaximum() {
        assertEquals(TimerSettings.MAX_SECONDS, DurationInput.toSeconds("999999"))
    }

    @Test
    fun fromSeconds_roundTripsThroughToSeconds() {
        listOf(1, 59, 60, 25 * 60, 3_661, TimerSettings.MAX_SECONDS).forEach { seconds ->
            assertEquals(seconds, DurationInput.toSeconds(DurationInput.fromSeconds(seconds)))
        }
        assertEquals("", DurationInput.fromSeconds(0))
    }
}
