package com.miyabi0619.pomodorotimer

import android.app.Application
import com.miyabi0619.pomodorotimer.timer.TimerController

class PomodoroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // プロセスがアラームや再起動で起こされた場合も、保存済みの状態を復元する
        TimerController.get(this).restore()
    }
}
