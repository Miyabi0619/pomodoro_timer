package com.miyabi0619.pomodorotimer.timer

/**
 * 数字キーによる時間入力。純正タイマーと同じく右詰めで入力し、`HH MM SS` の6桁で表す。
 * 入力中の値は「先頭の0を省いた数字列」(例: 15分 → "1500")で持つ。
 */
object DurationInput {
    const val MAX_DIGITS = 6

    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L

    fun fromSeconds(totalSeconds: Int): String {
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        val value = hours * 10_000 + minutes * 100 + seconds
        return if (value == 0L) "" else value.toString()
    }

    /** 桁数を超える入力は無視する。先頭が0のままなら何も起きない(0, 00 キー対策)。 */
    fun append(buffer: String, digits: String): String {
        val next = if (buffer.isEmpty()) digits.trimStart('0') else buffer + digits
        return if (next.length > MAX_DIGITS) buffer else next
    }

    fun delete(buffer: String): String = buffer.dropLast(1)

    /** 表示用に6桁へ0埋めする。 */
    fun padded(buffer: String): String = buffer.padStart(MAX_DIGITS, '0')

    /** 「90秒」のような繰り上がりは正規化し、上限(99:59:59)を超える分は切り詰める。 */
    fun toSeconds(buffer: String): Int {
        val padded = padded(buffer)
        val hours = padded.substring(0, 2).toLong()
        val minutes = padded.substring(2, 4).toLong()
        val seconds = padded.substring(4, 6).toLong()
        val total = hours * SECONDS_PER_HOUR + minutes * SECONDS_PER_MINUTE + seconds
        return total.coerceAtMost(TimerSettings.MAX_SECONDS.toLong()).toInt()
    }
}
