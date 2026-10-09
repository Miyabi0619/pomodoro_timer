package com.miyabi0619.pomodorotimer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miyabi0619.pomodorotimer.R
import com.miyabi0619.pomodorotimer.notification.TimerNotifier
import com.miyabi0619.pomodorotimer.timer.AlertMode
import com.miyabi0619.pomodorotimer.timer.Phase
import com.miyabi0619.pomodorotimer.timer.TimerEngine
import com.miyabi0619.pomodorotimer.timer.TimerSettings

private const val MILLIS_PER_SECOND = 1_000L

private val EDITABLE_PHASES = listOf(
    Phase.WORK to R.string.settings_work,
    Phase.SHORT_BREAK to R.string.settings_short_break,
    Phase.LONG_BREAK to R.string.settings_long_break,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: TimerSettings,
    onSettingsChange: (TimerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    // キーパッドは縦に長いので、最初から全開で表示する
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var editingPhase by rememberSaveable { mutableStateOf<Phase?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val phase = editingPhase
            if (phase == null) {
                SettingsList(settings, onEdit = { editingPhase = it }, onAlertModeChange = { onSettingsChange(settings.copy(alertMode = it)) })
            } else {
                DurationKeypad(
                    title = stringResource(R.string.settings_edit_duration, stringResource(labelRes(phase))),
                    confirmLabel = stringResource(R.string.keypad_confirm),
                    initialSeconds = settings.secondsOf(phase),
                    onConfirm = { seconds ->
                        onSettingsChange(settings.withSeconds(phase, seconds))
                        editingPhase = null
                    },
                    onCancel = { editingPhase = null },
                )
            }
        }
    }
}

@Composable
private fun SettingsList(
    settings: TimerSettings,
    onEdit: (Phase) -> Unit,
    onAlertModeChange: (AlertMode) -> Unit,
) {
    Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

    EDITABLE_PHASES.forEach { (phase, labelRes) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit(phase) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
            Text(
                text = TimerEngine.formatClock(settings.secondsOf(phase) * MILLIS_PER_SECOND),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    Text(stringResource(R.string.settings_alert_mode), style = MaterialTheme.typography.titleMedium)
    AlertModeSelector(settings.alertMode, onAlertModeChange)
}

private fun labelRes(phase: Phase): Int = EDITABLE_PHASES.first { it.first == phase }.second

@Composable
private fun AlertModeSelector(selected: AlertMode, onSelect: (AlertMode) -> Unit) {
    val modes = AlertMode.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
            ) {
                Text(
                    stringResource(
                        when (mode) {
                            AlertMode.SOUND -> R.string.alert_mode_sound
                            AlertMode.VIBRATION -> R.string.alert_mode_vibration
                        },
                    ),
                )
            }
        }
    }
}
