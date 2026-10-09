package com.miyabi0619.pomodorotimer.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.miyabi0619.pomodorotimer.R
import com.miyabi0619.pomodorotimer.timer.AlertMode
import com.miyabi0619.pomodorotimer.timer.Phase
import com.miyabi0619.pomodorotimer.timer.RunStatus
import com.miyabi0619.pomodorotimer.timer.TimerEngine
import com.miyabi0619.pomodorotimer.timer.TimerIntents
import com.miyabi0619.pomodorotimer.timer.TimerState

/**
 * 常駐の残り時間通知(ロック画面にも表示)と、フェーズ終了の通知を扱う。
 * 残り時間はシステム側の Chronometer カウントダウンが描画するため、アプリが停止していても進む。
 */
class TimerNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    fun refresh(state: TimerState) {
        if (state.status == RunStatus.IDLE) {
            manager.cancel(ID_ONGOING)
        } else {
            notify(ID_ONGOING, buildOngoing(state))
        }
    }

    fun cancelFinished() = manager.cancel(ID_FINISHED)

    fun notifyPhaseFinished(finished: Phase, next: TimerState, alertMode: AlertMode) {
        val channel = when (alertMode) {
            AlertMode.SOUND -> CHANNEL_ALERT_SOUND
            AlertMode.VIBRATION -> CHANNEL_ALERT_VIBRATION
        }
        val titleRes = if (finished == Phase.WORK) R.string.notification_work_finished else R.string.notification_break_finished
        val nextDuration = TimerEngine.formatClock(next.remainingMillis)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(titleRes))
            .setContentText(context.getString(R.string.notification_next_started, context.getString(phaseLabelRes(next.phase, running = false)), nextDuration))
            .setContentIntent(TimerIntents.openApp(context))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notify(ID_FINISHED, notification)
    }

    private fun buildOngoing(state: TimerState): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(phaseLabel(state))
            .setContentIntent(TimerIntents.openApp(context))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        val completed = context.getString(R.string.completed_sets, state.completedWorkCount)
        if (state.status == RunStatus.RUNNING) {
            builder
                .setContentText(completed)
                .setWhen(state.endTimeMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.action_pause), TimerIntents.action(context, TimerIntents.ACTION_PAUSE))
                .addAction(0, context.getString(R.string.action_skip), TimerIntents.action(context, TimerIntents.ACTION_SKIP))
        } else {
            val remaining = TimerEngine.formatClock(state.remainingMillis)
            builder
                .setContentText(context.getString(R.string.notification_paused, remaining, completed))
                .setShowWhen(false)
                .addAction(0, context.getString(R.string.action_resume), TimerIntents.action(context, TimerIntents.ACTION_RESUME))
                .addAction(0, context.getString(R.string.action_reset), TimerIntents.action(context, TimerIntents.ACTION_RESET))
        }
        return builder.build()
    }

    // 通知権限が無い場合、システム側で破棄されるだけなので呼び出し時の事前チェックは不要
    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: Notification) = manager.notify(id, notification)

    private fun phaseLabel(state: TimerState): String = context.getString(phaseLabelRes(state))

    private fun createChannels() {
        val systemManager = context.getSystemService(NotificationManager::class.java)

        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            context.getString(R.string.channel_ongoing),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }

        val sound = NotificationChannel(
            CHANNEL_ALERT_SOUND,
            context.getString(R.string.channel_alert_sound),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), attributes)
            enableVibration(false)
        }

        val vibration = NotificationChannel(
            CHANNEL_ALERT_VIBRATION,
            context.getString(R.string.channel_alert_vibration),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION_PATTERN
        }

        systemManager.createNotificationChannels(listOf(ongoing, sound, vibration))
    }

    companion object {
        private const val ID_ONGOING = 1
        private const val ID_FINISHED = 2
        private const val CHANNEL_ONGOING = "timer_ongoing"
        private const val CHANNEL_ALERT_SOUND = "timer_alert_sound"
        private const val CHANNEL_ALERT_VIBRATION = "timer_alert_vibration"
        private val VIBRATION_PATTERN = longArrayOf(0, 400, 200, 400, 200, 400)

        @StringRes
        /** 「〜中」はカウントダウンが進んでいるときだけ。開始前・一時停止中は「作業」「休憩」。 */
        fun phaseLabelRes(phase: Phase, running: Boolean): Int = when (phase) {
            Phase.WORK -> if (running) R.string.phase_work_running else R.string.phase_work
            Phase.SHORT_BREAK -> if (running) R.string.phase_short_break_running else R.string.phase_short_break
            Phase.LONG_BREAK -> if (running) R.string.phase_long_break_running else R.string.phase_long_break
        }

        fun phaseLabelRes(state: TimerState): Int =
            phaseLabelRes(state.phase, running = state.status == RunStatus.RUNNING)
    }
}
