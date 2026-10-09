package com.miyabi0619.pomodorotimer.timer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.miyabi0619.pomodorotimer.MainActivity
import com.miyabi0619.pomodorotimer.receiver.TimerActionReceiver

/** 通知・ウィジェットから共通で使う PendingIntent の生成。 */
object TimerIntents {
    const val ACTION_TOGGLE = "com.miyabi0619.pomodorotimer.action.TOGGLE"
    const val ACTION_PAUSE = "com.miyabi0619.pomodorotimer.action.PAUSE"
    const val ACTION_RESUME = "com.miyabi0619.pomodorotimer.action.RESUME"
    const val ACTION_RESET = "com.miyabi0619.pomodorotimer.action.RESET"
    const val ACTION_SKIP = "com.miyabi0619.pomodorotimer.action.SKIP"

    private const val OPEN_APP_REQUEST_CODE = 0
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun action(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, TimerActionReceiver::class.java).setAction(action),
        FLAGS,
    )

    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        OPEN_APP_REQUEST_CODE,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        FLAGS,
    )
}
