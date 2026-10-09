package com.miyabi0619.pomodorotimer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miyabi0619.pomodorotimer.R

/** 実行中・一時停止中・待機中のいずれでも、現在フェーズの残り時間に入力分を足す。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeSheet(
    onAdd: (seconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            DurationKeypad(
                title = stringResource(R.string.add_time_title),
                confirmLabel = stringResource(R.string.add_time_confirm),
                initialSeconds = 0,
                onConfirm = { seconds ->
                    onAdd(seconds)
                    onDismiss()
                },
                onCancel = onDismiss,
            )
        }
    }
}
