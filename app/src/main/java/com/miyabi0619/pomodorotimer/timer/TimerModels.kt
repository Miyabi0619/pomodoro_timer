package com.miyabi0619.pomodorotimer.timer

enum class Phase { WORK, SHORT_BREAK, LONG_BREAK }

enum class RunStatus { IDLE, RUNNING, PAUSED }

enum class AlertMode { SOUND, VIBRATION }

data class TimerSettings(
    val workSeconds: Int = DEFAULT_WORK_SECONDS,
    val shortBreakSeconds: Int = DEFAULT_SHORT_BREAK_SECONDS,
    val longBreakSeconds: Int = DEFAULT_LONG_BREAK_SECONDS,
    val alertMode: AlertMode = AlertMode.SOUND,
) {
    fun secondsOf(phase: Phase): Int = when (phase) {
        Phase.WORK -> workSeconds
        Phase.SHORT_BREAK -> shortBreakSeconds
        Phase.LONG_BREAK -> longBreakSeconds
    }

    fun withSeconds(phase: Phase, seconds: Int): TimerSettings = when (phase) {
        Phase.WORK -> copy(workSeconds = seconds)
        Phase.SHORT_BREAK -> copy(shortBreakSeconds = seconds)
        Phase.LONG_BREAK -> copy(longBreakSeconds = seconds)
    }

    companion object {
        const val DEFAULT_WORK_SECONDS = 25 * 60
        const val DEFAULT_SHORT_BREAK_SECONDS = 5 * 60
        const val DEFAULT_LONG_BREAK_SECONDS = 15 * 60

        const val MIN_SECONDS = 1

        /** 99時間59分59秒 */
        const val MAX_SECONDS = 99 * 3600 + 59 * 60 + 59
    }
}

/**
 * タイマーの永続化対象となる状態。
 *
 * - [RunStatus.RUNNING]: [endTimeMillis] (壁時計) が有効。残り時間は `endTimeMillis - now` で求める。
 * - [RunStatus.IDLE] / [RunStatus.PAUSED]: [remainingMillis] が有効。
 *
 * [totalMillis] は現在フェーズの合計時間(進捗リングの基準)。時間を追加した分も含む。
 */
data class TimerState(
    val phase: Phase,
    val status: RunStatus,
    val completedWorkCount: Int,
    val endTimeMillis: Long,
    val remainingMillis: Long,
    val totalMillis: Long,
)
