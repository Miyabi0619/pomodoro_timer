package com.miyabi0619.pomodorotimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyabi0619.pomodorotimer.R
import com.miyabi0619.pomodorotimer.timer.DurationInput

private val KeyHeight = 56.dp
private val PRESET_MINUTES = listOf(1, 5, 10, 15)
private val KEY_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
)

/**
 * 純正タイマー風の時間入力。`HH MM SS` 表示、プリセット、確定ボタン、テンキーの順に並べる。
 * 確定される値は秒数で、0秒の間は確定できない。
 */
@Composable
fun DurationKeypad(
    title: String,
    confirmLabel: String,
    initialSeconds: Int,
    onConfirm: (seconds: Int) -> Unit,
    onCancel: () -> Unit,
) {
    var buffer by rememberSaveable { mutableStateOf(DurationInput.fromSeconds(initialSeconds)) }
    val seconds = DurationInput.toSeconds(buffer)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.keypad_cancel)) }
        }

        DurationDisplay(buffer)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            PRESET_MINUTES.forEach { minutes ->
                AssistChip(
                    onClick = { buffer = DurationInput.fromSeconds(minutes * 60) },
                    label = { Text(stringResource(R.string.keypad_preset, minutes)) },
                )
            }
        }

        Button(
            onClick = { onConfirm(seconds) },
            enabled = seconds > 0,
            modifier = Modifier.fillMaxWidth().height(KeyHeight),
        ) {
            Text(confirmLabel, style = MaterialTheme.typography.titleMedium)
        }

        KEY_ROWS.forEach { row ->
            KeyRow {
                row.forEach { digit -> DigitKey(digit, Modifier.weight(1f)) { buffer = DurationInput.append(buffer, digit) } }
            }
        }
        KeyRow {
            DigitKey("00", Modifier.weight(1f)) { buffer = DurationInput.append(buffer, "00") }
            DigitKey("0", Modifier.weight(1f)) { buffer = DurationInput.append(buffer, "0") }
            FilledTonalButton(
                onClick = { buffer = DurationInput.delete(buffer) },
                enabled = buffer.isNotEmpty(),
                modifier = Modifier.weight(1f).height(KeyHeight),
            ) {
                Icon(painterResource(R.drawable.ic_backspace), contentDescription = stringResource(R.string.keypad_delete))
            }
        }
    }
}

/** `00 00 00` と `時間 分 秒` のラベル。まだ入力されていない桁は薄く表示する。 */
@Composable
private fun DurationDisplay(buffer: String) {
    val padded = DurationInput.padded(buffer)
    val typedFrom = DurationInput.MAX_DIGITS - buffer.length
    val typedColor = MaterialTheme.colorScheme.onSurface
    val untypedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    fun group(start: Int): AnnotatedString = buildAnnotatedString {
        for (index in start until start + 2) {
            withStyle(SpanStyle(color = if (index >= typedFrom) typedColor else untypedColor)) { append(padded[index]) }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(0 to R.string.keypad_hour, 2 to R.string.keypad_minute, 4 to R.string.keypad_second).forEach { (start, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = group(start),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun DigitKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(KeyHeight)) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}
