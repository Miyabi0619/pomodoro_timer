package com.miyabi0619.pomodorotimer.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.miyabi0619.pomodorotimer.receiver.PhaseAlarmReceiver

/** フェーズ終了時刻に BroadcastReceiver を起こす。Doze 中でも発火する exact alarm を使う。 */
class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun sync(state: TimerState) {
        if (state.status == RunStatus.RUNNING) schedule(state.endTimeMillis) else cancel()
    }

    private fun schedule(triggerAtMillis: Long) {
        val pendingIntent = pendingIntent()
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // 権限が取り消されている場合は精度を落としてでも発火させる
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancel() = alarmManager.cancel(pendingIntent())

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, PhaseAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val REQUEST_CODE = 100
    }
}
