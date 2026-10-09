package com.miyabi0619.pomodorotimer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.miyabi0619.pomodorotimer.timer.RunStatus
import com.miyabi0619.pomodorotimer.timer.TimerController
import com.miyabi0619.pomodorotimer.ui.TimerScreen
import com.miyabi0619.pomodorotimer.ui.theme.PomodorotimerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val controller by lazy { TimerController.get(this) }
    private val nowMillis = mutableLongStateOf(System.currentTimeMillis())

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* 拒否時は通知なしで動作を続ける */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        startTicker()

        setContent {
            val state by controller.state.collectAsState()
            val settings by controller.settings.collectAsState()
            PomodorotimerTheme {
                TimerScreen(
                    state = state,
                    settings = settings,
                    nowMillis = nowMillis.longValue,
                    onTogglePlay = controller::togglePlayPause,
                    onReset = controller::reset,
                    onSkip = controller::skip,
                    onAddTime = controller::addTime,
                    onSettingsChange = controller::updateSettings,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 画面が見えていなかった間に期限を過ぎていた場合に備えて状態を確定させる
        controller.restore()
    }

    /** 画面表示中だけ、残り時間の描画用に現在時刻を更新する。計時そのものは終了予定時刻で行う。 */
    private fun startTicker() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (controller.state.value.status == RunStatus.RUNNING) {
                        nowMillis.longValue = System.currentTimeMillis()
                    }
                    delay(TICK_MILLIS)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val TICK_MILLIS = 200L
    }
}
