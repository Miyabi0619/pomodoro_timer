package com.miyabi0619.pomodorotimer.timer

import java.util.Locale

/** 状態遷移の純粋関数群。副作用(保存・アラーム・通知)は [TimerController] が担う。 */
object TimerEngine {
    const val WORKS_PER_CYCLE = 4

    private const val MILLIS_PER_SECOND = 1_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val SECONDS_PER_HOUR = 3_600L
    private const val MAX_REMAINING_MILLIS = TimerSettings.MAX_SECONDS * MILLIS_PER_SECOND

    /** これ以上遅れて終了処理に入った場合は、遅れを引き継がず「今」から次フェーズを始める。 */
    private const val CARRY_OVER_LIMIT_MILLIS = 60_000L

    fun durationMillis(phase: Phase, settings: TimerSettings): Long =
        settings.secondsOf(phase) * MILLIS_PER_SECOND

    fun initial(settings: TimerSettings): TimerState = idle(Phase.WORK, 0, settings)

    fun remainingMillis(state: TimerState, now: Long): Long =
        if (state.status == RunStatus.RUNNING) {
            (state.endTimeMillis - now).coerceAtLeast(0)
        } else {
            state.remainingMillis
        }

    /** 開始・再開。IDLE / PAUSED から RUNNING へ。 */
    fun play(state: TimerState, now: Long): TimerState =
        if (state.status == RunStatus.RUNNING) {
            state
        } else {
            state.copy(status = RunStatus.RUNNING, endTimeMillis = now + state.remainingMillis)
        }

    fun pause(state: TimerState, now: Long): TimerState =
        if (state.status == RunStatus.RUNNING) {
            state.copy(
                status = RunStatus.PAUSED,
                endTimeMillis = 0,
                remainingMillis = remainingMillis(state, now),
            )
        } else {
            state
        }

    /**
     * 手動スキップ。完了回数は増やさない。作業をスキップした場合は常に短い休憩へ進む。
     * 実行中ならそのまま次フェーズを開始し、それ以外は待機状態で次フェーズへ移る。
     */
    fun skip(state: TimerState, settings: TimerSettings, now: Long): TimerState {
        val next = if (state.phase == Phase.WORK) Phase.SHORT_BREAK else Phase.WORK
        return if (state.status == RunStatus.RUNNING) {
            running(next, state.completedWorkCount, now, settings)
        } else {
            idle(next, state.completedWorkCount, settings)
        }
    }

    /** フェーズが自然に終了したときの遷移。次フェーズは自動で開始される。 */
    fun complete(state: TimerState, settings: TimerSettings, now: Long): TimerState {
        val completed = if (state.phase == Phase.WORK) state.completedWorkCount + 1 else state.completedWorkCount
        val next = when (state.phase) {
            Phase.WORK -> if (completed % WORKS_PER_CYCLE == 0) Phase.LONG_BREAK else Phase.SHORT_BREAK
            Phase.SHORT_BREAK, Phase.LONG_BREAK -> Phase.WORK
        }
        // 通常は予定終了時刻から次を始めて誤差を蓄積させない。大幅に遅れた場合は今から。
        val startAt = if (now - state.endTimeMillis <= CARRY_OVER_LIMIT_MILLIS) state.endTimeMillis else now
        return running(next, completed, startAt, settings)
    }

    /**
     * 現在フェーズの残り時間を延ばす(保存済みの設定は変えない)。
     * 実行中なら終了予定時刻を後ろへずらす。上限(99:59:59)を超える分は切り捨てる。
     */
    fun addTime(state: TimerState, addMillis: Long, now: Long): TimerState {
        if (addMillis <= 0) return state
        val current = remainingMillis(state, now)
        val extended = (current + addMillis).coerceAtMost(MAX_REMAINING_MILLIS)
        val added = extended - current
        if (added <= 0) return state
        val total = state.totalMillis + added
        return if (state.status == RunStatus.RUNNING) {
            state.copy(endTimeMillis = now + extended, totalMillis = total)
        } else {
            state.copy(remainingMillis = extended, totalMillis = total)
        }
    }

    /** 設定変更の反映。実行中のフェーズには影響させない。 */
    fun applySettings(state: TimerState, settings: TimerSettings): TimerState {
        val duration = durationMillis(state.phase, settings)
        return when (state.status) {
            RunStatus.RUNNING -> state
            RunStatus.IDLE -> state.copy(remainingMillis = duration, totalMillis = duration)
            RunStatus.PAUSED -> state.copy(remainingMillis = minOf(state.remainingMillis, duration), totalMillis = duration)
        }
    }

    /**
     * 残り時間を「分:秒」(1時間以上は「時:分:秒」)に整形する。
     * 秒は切り上げ(開始直後に 25:00 と表示するため)。
     */
    fun formatClock(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = totalSeconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun idle(phase: Phase, completed: Int, settings: TimerSettings) = TimerState(
        phase = phase,
        status = RunStatus.IDLE,
        completedWorkCount = completed,
        endTimeMillis = 0,
        remainingMillis = durationMillis(phase, settings),
        totalMillis = durationMillis(phase, settings),
    )

    private fun running(phase: Phase, completed: Int, startAt: Long, settings: TimerSettings): TimerState {
        val duration = durationMillis(phase, settings)
        return TimerState(
            phase = phase,
            status = RunStatus.RUNNING,
            completedWorkCount = completed,
            endTimeMillis = startAt + duration,
            remainingMillis = duration,
            totalMillis = duration,
        )
    }
}
