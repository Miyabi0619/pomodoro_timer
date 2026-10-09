package com.miyabi0619.pomodorotimer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.ColorStateList
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorRes
import com.miyabi0619.pomodorotimer.R
import com.miyabi0619.pomodorotimer.notification.TimerNotifier
import com.miyabi0619.pomodorotimer.timer.Phase
import com.miyabi0619.pomodorotimer.timer.RunStatus
import com.miyabi0619.pomodorotimer.timer.TimerController
import com.miyabi0619.pomodorotimer.timer.TimerEngine
import com.miyabi0619.pomodorotimer.timer.TimerIntents
import com.miyabi0619.pomodorotimer.timer.TimerState

/**
 * ホーム画面ウィジェット。実行中の残り時間は Chronometer のカウントダウンで描画されるため、
 * 毎秒の更新は不要で、状態が変わったときだけ [refresh] で作り直す。
 */
class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val state = TimerController.get(context).state.value
        appWidgetManager.updateAppWidget(appWidgetIds, buildViews(context, state))
    }

    companion object {
        fun refresh(context: Context, state: TimerState) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, buildViews(context, state))
        }

        private fun buildViews(context: Context, state: TimerState): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_timer)
            val palette = palette(state.phase)
            val now = System.currentTimeMillis()

            views.setColorStateList(R.id.widget_root, "setBackgroundTintList", tint(context, palette.containerLight), tint(context, palette.containerDark))
            views.setColorInt(R.id.widget_phase, "setTextColor", context.getColor(palette.onContainerLight), context.getColor(palette.onContainerDark))
            views.setColorInt(R.id.widget_chronometer, "setTextColor", context.getColor(palette.onContainerLight), context.getColor(palette.onContainerDark))
            views.setColorInt(R.id.widget_time, "setTextColor", context.getColor(palette.onContainerLight), context.getColor(palette.onContainerDark))
            views.setColorInt(R.id.widget_sets, "setTextColor", context.getColor(palette.onContainerLight), context.getColor(palette.onContainerDark))
            views.setColorStateList(R.id.widget_toggle, "setBackgroundTintList", tint(context, palette.accentLight), tint(context, palette.accentDark))
            views.setColorStateList(R.id.widget_toggle, "setImageTintList", tint(context, palette.onAccentLight), tint(context, palette.onAccentDark))

            views.setTextViewText(R.id.widget_phase, context.getString(TimerNotifier.phaseLabelRes(state)))
            views.setTextViewText(R.id.widget_sets, context.getString(R.string.completed_sets, state.completedWorkCount))

            val running = state.status == RunStatus.RUNNING
            if (running) {
                val base = SystemClock.elapsedRealtime() + (state.endTimeMillis - now)
                views.setChronometerCountDown(R.id.widget_chronometer, true)
                views.setChronometer(R.id.widget_chronometer, base, null, true)
            } else {
                views.setChronometer(R.id.widget_chronometer, SystemClock.elapsedRealtime(), null, false)
                views.setTextViewText(R.id.widget_time, TimerEngine.formatClock(TimerEngine.remainingMillis(state, now)))
            }
            views.setViewVisibility(R.id.widget_chronometer, if (running) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_time, if (running) View.GONE else View.VISIBLE)

            views.setImageViewResource(R.id.widget_toggle, if (running) R.drawable.ic_pause else R.drawable.ic_play)
            views.setContentDescription(
                R.id.widget_toggle,
                context.getString(
                    when (state.status) {
                        RunStatus.RUNNING -> R.string.action_pause
                        RunStatus.PAUSED -> R.string.action_resume
                        RunStatus.IDLE -> R.string.action_start
                    },
                ),
            )
            views.setOnClickPendingIntent(R.id.widget_toggle, TimerIntents.action(context, TimerIntents.ACTION_TOGGLE))
            views.setOnClickPendingIntent(R.id.widget_root, TimerIntents.openApp(context))
            return views
        }

        private fun tint(context: Context, @ColorRes color: Int) = ColorStateList.valueOf(context.getColor(color))

        /** Material 3 の動的カラーに対応するシステムのトーン色。ライト/ダークで別々に指定する。 */
        private class Palette(
            @ColorRes val containerLight: Int,
            @ColorRes val containerDark: Int,
            @ColorRes val onContainerLight: Int,
            @ColorRes val onContainerDark: Int,
            @ColorRes val accentLight: Int,
            @ColorRes val accentDark: Int,
            @ColorRes val onAccentLight: Int,
            @ColorRes val onAccentDark: Int,
        )

        private fun palette(phase: Phase): Palette = when (phase) {
            Phase.WORK -> Palette(
                android.R.color.system_accent1_100, android.R.color.system_accent1_700,
                android.R.color.system_accent1_900, android.R.color.system_accent1_100,
                android.R.color.system_accent1_600, android.R.color.system_accent1_200,
                android.R.color.system_accent1_0, android.R.color.system_accent1_800,
            )
            Phase.SHORT_BREAK -> Palette(
                android.R.color.system_accent3_100, android.R.color.system_accent3_700,
                android.R.color.system_accent3_900, android.R.color.system_accent3_100,
                android.R.color.system_accent3_600, android.R.color.system_accent3_200,
                android.R.color.system_accent3_0, android.R.color.system_accent3_800,
            )
            Phase.LONG_BREAK -> Palette(
                android.R.color.system_accent2_100, android.R.color.system_accent2_700,
                android.R.color.system_accent2_900, android.R.color.system_accent2_100,
                android.R.color.system_accent2_600, android.R.color.system_accent2_200,
                android.R.color.system_accent2_0, android.R.color.system_accent2_800,
            )
        }
    }
}
