package com.miyabi0619.pomodorotimer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyabi0619.pomodorotimer.R
import com.miyabi0619.pomodorotimer.notification.TimerNotifier
import com.miyabi0619.pomodorotimer.timer.Phase
import com.miyabi0619.pomodorotimer.timer.RunStatus
import com.miyabi0619.pomodorotimer.timer.TimerEngine
import com.miyabi0619.pomodorotimer.timer.TimerSettings
import com.miyabi0619.pomodorotimer.timer.TimerState

private val RingSize = 300.dp
private val RingStroke = 14.dp
private val ResetSkipButtonSize = 56.dp
private val DotSize = 10.dp
private val ClockFontSize = 68.sp
private val LongClockFontSize = 48.sp
private const val MILLIS_PER_HOUR = 3_600_000L

/** フェーズごとの配色。OS(動的カラー)の色を、作業=primary / 短い休憩=tertiary / 長い休憩=secondary に割り当てる。 */
private data class PhaseColors(val container: Color, val onContainer: Color, val accent: Color, val onAccent: Color)

@Composable
private fun phaseColors(phase: Phase): PhaseColors {
    val scheme = MaterialTheme.colorScheme
    val target = when (phase) {
        Phase.WORK -> PhaseColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary, scheme.onPrimary)
        Phase.SHORT_BREAK -> PhaseColors(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary, scheme.onTertiary)
        Phase.LONG_BREAK -> PhaseColors(scheme.secondaryContainer, scheme.onSecondaryContainer, scheme.secondary, scheme.onSecondary)
    }
    return PhaseColors(
        container = animateColorAsState(target.container, label = "container").value,
        onContainer = animateColorAsState(target.onContainer, label = "onContainer").value,
        accent = animateColorAsState(target.accent, label = "accent").value,
        onAccent = animateColorAsState(target.onAccent, label = "onAccent").value,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    state: TimerState,
    settings: TimerSettings,
    nowMillis: Long,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onAddTime: (seconds: Int) -> Unit,
    onSettingsChange: (TimerSettings) -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAddTime by rememberSaveable { mutableStateOf(false) }
    val colors = phaseColors(state.phase)

    // 再開・開始の直後は、ティッカーの時刻が停止前の古い値のままなので、
    // 新しい状態を初めて描画する時点の時刻を下限にして残り時間が跳ねるのを防ぐ。
    val stateEnteredAt = remember(state) { System.currentTimeMillis() }
    val displayNow = maxOf(nowMillis, stateEnteredAt)

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.settings_open))
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TimerRing(state, displayNow, colors, onClockClick = { showAddTime = true })
                Spacer(Modifier.height(48.dp))
                Controls(state, colors, onTogglePlay, onReset, onSkip)
            }
        }
    }

    if (showSettings) {
        SettingsSheet(settings, onSettingsChange, onDismiss = { showSettings = false })
    }
    if (showAddTime) {
        AddTimeSheet(onAdd = onAddTime, onDismiss = { showAddTime = false })
    }
}

@Composable
private fun TimerRing(state: TimerState, nowMillis: Long, colors: PhaseColors, onClockClick: () -> Unit) {
    val remaining = TimerEngine.remainingMillis(state, nowMillis)
    val total = state.totalMillis.coerceAtLeast(1)
    val fraction = (remaining.toFloat() / total).coerceIn(0f, 1f)

    Box(Modifier.size(RingSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = colors.container,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(TimerNotifier.phaseLabelRes(state)),
                style = MaterialTheme.typography.titleLarge,
                color = colors.accent,
            )
            Text(
                text = TimerEngine.formatClock(remaining),
                // 1時間以上は「H:MM:SS」で桁が増えるので小さくして輪の内側に収める
                fontSize = if (remaining >= MILLIS_PER_HOUR) LongClockFontSize else ClockFontSize,
                fontWeight = FontWeight.Light,
                // 桁幅を固定して、秒が変わるたびに文字が横にぶれないようにする
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.clickable(onClickLabel = stringResource(R.string.add_time_click_label), onClick = onClockClick),
            )
            ProgressDots(state, colors)
            Text(
                text = stringResource(R.string.completed_sets, state.completedWorkCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** 現在のサイクル(4回)の進み具合。長い休憩中は4つすべて点灯する。 */
@Composable
private fun ProgressDots(state: TimerState, colors: PhaseColors) {
    val filled = if (state.phase == Phase.LONG_BREAK) {
        TimerEngine.WORKS_PER_CYCLE
    } else {
        state.completedWorkCount % TimerEngine.WORKS_PER_CYCLE
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        repeat(TimerEngine.WORKS_PER_CYCLE) { index ->
            Box(
                Modifier
                    .size(DotSize)
                    .background(if (index < filled) colors.accent else colors.container, CircleShape),
            )
        }
    }
}

@Composable
private fun Controls(
    state: TimerState,
    colors: PhaseColors,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
) {
    val (iconRes, labelRes) = when (state.status) {
        RunStatus.RUNNING -> R.drawable.ic_pause to R.string.action_pause
        RunStatus.PAUSED -> R.drawable.ic_play to R.string.action_resume
        RunStatus.IDLE -> R.drawable.ic_play to R.string.action_start
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onReset, modifier = Modifier.size(ResetSkipButtonSize)) {
            Icon(painterResource(R.drawable.ic_restart), contentDescription = stringResource(R.string.action_reset))
        }
        ExtendedFloatingActionButton(
            onClick = onTogglePlay,
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            icon = { Icon(painterResource(iconRes), contentDescription = null) },
            text = { Text(stringResource(labelRes), style = MaterialTheme.typography.titleMedium) },
        )
        FilledTonalIconButton(onClick = onSkip, modifier = Modifier.size(ResetSkipButtonSize)) {
            Icon(painterResource(R.drawable.ic_skip_next), contentDescription = stringResource(R.string.action_skip))
        }
    }
}
