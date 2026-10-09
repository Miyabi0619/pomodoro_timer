package com.miyabi0619.pomodorotimer.timer

import android.content.Context
import androidx.core.content.edit

/** 状態と設定を SharedPreferences に保存する。再起動・プロセス終了後の復元用。 */
class TimerStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadState(): TimerState? {
        val phase = prefs.getString(KEY_PHASE, null)?.toEnumOrNull<Phase>() ?: return null
        val status = prefs.getString(KEY_STATUS, null)?.toEnumOrNull<RunStatus>() ?: return null
        val remaining = prefs.getLong(KEY_REMAINING, 0)
        return TimerState(
            phase = phase,
            status = status,
            completedWorkCount = prefs.getInt(KEY_COMPLETED, 0),
            endTimeMillis = prefs.getLong(KEY_END_TIME, 0),
            remainingMillis = remaining,
            // 合計時間の保存が無い旧バージョンのデータでは、残り時間を合計とみなす
            totalMillis = prefs.getLong(KEY_TOTAL, remaining),
        )
    }

    fun saveState(state: TimerState) = prefs.edit {
        putString(KEY_PHASE, state.phase.name)
        putString(KEY_STATUS, state.status.name)
        putInt(KEY_COMPLETED, state.completedWorkCount)
        putLong(KEY_END_TIME, state.endTimeMillis)
        putLong(KEY_REMAINING, state.remainingMillis)
        putLong(KEY_TOTAL, state.totalMillis)
    }

    fun loadSettings(): TimerSettings {
        val defaults = TimerSettings()
        return TimerSettings(
            workSeconds = loadSeconds(KEY_WORK_SEC, KEY_WORK_MIN, defaults.workSeconds),
            shortBreakSeconds = loadSeconds(KEY_SHORT_SEC, KEY_SHORT_MIN, defaults.shortBreakSeconds),
            longBreakSeconds = loadSeconds(KEY_LONG_SEC, KEY_LONG_MIN, defaults.longBreakSeconds),
            alertMode = prefs.getString(KEY_ALERT_MODE, null)?.toEnumOrNull<AlertMode>() ?: defaults.alertMode,
        )
    }

    fun saveSettings(settings: TimerSettings) = prefs.edit {
        putInt(KEY_WORK_SEC, settings.workSeconds)
        putInt(KEY_SHORT_SEC, settings.shortBreakSeconds)
        putInt(KEY_LONG_SEC, settings.longBreakSeconds)
        putString(KEY_ALERT_MODE, settings.alertMode.name)
    }

    /** 秒単位の値を優先し、無ければ旧バージョンの「分」単位の値を引き継ぐ。 */
    private fun loadSeconds(secondsKey: String, legacyMinutesKey: String, default: Int): Int {
        val seconds = when {
            prefs.contains(secondsKey) -> prefs.getInt(secondsKey, default)
            prefs.contains(legacyMinutesKey) -> prefs.getInt(legacyMinutesKey, default / SECONDS_PER_MINUTE) * SECONDS_PER_MINUTE
            else -> default
        }
        return seconds.coerceIn(TimerSettings.MIN_SECONDS, TimerSettings.MAX_SECONDS)
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
        enumValues<T>().firstOrNull { it.name == this }

    private companion object {
        const val PREFS_NAME = "pomodoro_timer"
        const val KEY_PHASE = "phase"
        const val KEY_STATUS = "status"
        const val KEY_COMPLETED = "completed_work_count"
        const val KEY_END_TIME = "end_time_millis"
        const val KEY_REMAINING = "remaining_millis"
        const val KEY_TOTAL = "total_millis"
        const val SECONDS_PER_MINUTE = 60
        const val KEY_WORK_SEC = "work_seconds"
        const val KEY_SHORT_SEC = "short_break_seconds"
        const val KEY_LONG_SEC = "long_break_seconds"
        const val KEY_WORK_MIN = "work_minutes"
        const val KEY_SHORT_MIN = "short_break_minutes"
        const val KEY_LONG_MIN = "long_break_minutes"
        const val KEY_ALERT_MODE = "alert_mode"
    }
}
