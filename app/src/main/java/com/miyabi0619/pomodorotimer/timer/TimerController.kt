package com.miyabi0619.pomodorotimer.timer

import android.content.Context
import com.miyabi0619.pomodorotimer.notification.TimerNotifier
import com.miyabi0619.pomodorotimer.widget.TimerWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 状態遷移の副作用(保存・アラーム再設定・通知・ウィジェット更新)を一箇所に集約する。
 * Activity・BroadcastReceiver・Application のいずれからでも同じ手順で操作できる。
 */
class TimerController private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val store = TimerStore(appContext)
    private val scheduler = AlarmScheduler(appContext)
    private val notifier = TimerNotifier(appContext)

    private val settingsFlow = MutableStateFlow(store.loadSettings())
    private val stateFlow = MutableStateFlow(store.loadState() ?: TimerEngine.initial(settingsFlow.value))

    val settings: StateFlow<TimerSettings> = settingsFlow.asStateFlow()
    val state: StateFlow<TimerState> = stateFlow.asStateFlow()

    fun togglePlayPause() = transition { current, now ->
        if (current.status == RunStatus.RUNNING) TimerEngine.pause(current, now) else TimerEngine.play(current, now)
    }

    fun pause() = transition(TimerEngine::pause)

    fun resume() = transition(TimerEngine::play)

    fun reset() = transition { _, _ -> TimerEngine.initial(settingsFlow.value) }

    fun skip() = transition { current, now -> TimerEngine.skip(current, settingsFlow.value, now) }

    fun addTime(seconds: Int) = transition { current, now -> TimerEngine.addTime(current, seconds * MILLIS_PER_SECOND, now) }

    @Synchronized
    fun updateSettings(newSettings: TimerSettings) {
        if (newSettings == settingsFlow.value) return
        settingsFlow.value = newSettings
        store.saveSettings(newSettings)
        val adjusted = TimerEngine.applySettings(stateFlow.value, newSettings)
        if (adjusted != stateFlow.value) commit(adjusted, finishedPhase = null)
    }

    /** アラーム受信時。ユーザー操作で既に状態が変わっている場合は何もしない。 */
    @Synchronized
    fun onPhaseAlarm() {
        completeIfDue()
    }

    /**
     * 起動・再起動・時刻変更・アップデート後に呼ぶ。
     * 停止中に期限を過ぎていれば遷移を完了させ、そうでなければアラームと通知を張り直す。
     */
    @Synchronized
    fun restore() {
        if (!completeIfDue()) commit(stateFlow.value, finishedPhase = null, cancelFinishedNotification = false)
    }

    @Synchronized
    private fun transition(block: (TimerState, Long) -> TimerState) {
        val current = stateFlow.value
        val next = block(current, System.currentTimeMillis())
        if (next != current) commit(next, finishedPhase = null)
    }

    private fun completeIfDue(): Boolean {
        val current = stateFlow.value
        val now = System.currentTimeMillis()
        if (current.status != RunStatus.RUNNING || current.endTimeMillis > now + DUE_TOLERANCE_MILLIS) return false
        commit(TimerEngine.complete(current, settingsFlow.value, now), finishedPhase = current.phase)
        return true
    }

    private fun commit(
        next: TimerState,
        finishedPhase: Phase?,
        cancelFinishedNotification: Boolean = true,
    ) {
        stateFlow.value = next
        store.saveState(next)
        scheduler.sync(next)
        notifier.refresh(next)
        TimerWidgetProvider.refresh(appContext, next)
        when {
            finishedPhase != null -> notifier.notifyPhaseFinished(finishedPhase, next, settingsFlow.value.alertMode)
            cancelFinishedNotification -> notifier.cancelFinished()
        }
    }

    companion object {
        private const val DUE_TOLERANCE_MILLIS = 1_000L
        private const val MILLIS_PER_SECOND = 1_000L

        @Volatile
        private var instance: TimerController? = null

        fun get(context: Context): TimerController =
            instance ?: synchronized(this) {
                instance ?: TimerController(context).also { instance = it }
            }
    }
}
