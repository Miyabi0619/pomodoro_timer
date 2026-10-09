package com.miyabi0619.pomodorotimer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.miyabi0619.pomodorotimer.timer.TimerController
import com.miyabi0619.pomodorotimer.timer.TimerIntents

/** フェーズ終了時刻に AlarmManager から起こされる。 */
class PhaseAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerController.get(context).onPhaseAlarm()
    }
}

/** 通知・ウィジェットのボタン操作。 */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val controller = TimerController.get(context)
        when (intent.action) {
            TimerIntents.ACTION_TOGGLE -> controller.togglePlayPause()
            TimerIntents.ACTION_PAUSE -> controller.pause()
            TimerIntents.ACTION_RESUME -> controller.resume()
            TimerIntents.ACTION_RESET -> controller.reset()
            TimerIntents.ACTION_SKIP -> controller.skip()
        }
    }
}

/** 再起動・時刻変更・アップデート後にアラームと通知を張り直す(アラームは再起動で消えるため)。 */
class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TimerController.get(context).restore()
    }
}
