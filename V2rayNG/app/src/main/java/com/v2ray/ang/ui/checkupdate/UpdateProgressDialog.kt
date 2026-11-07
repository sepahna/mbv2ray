package com.v2ray.ang.ui.checkupdate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.handler.AppUpdateUi

@Composable
fun UpdateProgressDialog(
    state: AppUpdateUi,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
) {
    if (state.awaitingChoice) {
        UpdateOfferDialog(state = state, onUpdateNow = onUpdateNow, onLater = onLater)
    } else {
        UpdateDownloadDialog(state = state, onDismiss = onLater)
    }
}

@Composable
private fun UpdateOfferDialog(
    state: AppUpdateUi,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit,
) {
    val notes = state.notes.ifBlank { stringResource(R.string.update_no_release_notes) }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_new_version_found, state.version)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = stringResource(R.string.update_whats_new),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = notes,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdateNow) {
                Text(stringResource(R.string.update_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.update_later))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun UpdateDownloadDialog(
    state: AppUpdateUi,
    onDismiss: () -> Unit,
) {
    val percent = if (state.total > 0) {
        ((state.received * 100) / state.total).toInt().coerceIn(0, 100)
    } else {
        null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.update_new_version_found, state.version))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        state.error != null -> stringResource(R.string.update_download_failed)
                        state.finished -> stringResource(R.string.update_install_ready)
                        else -> stringResource(R.string.update_downloading)
                    }
                )
                if (state.error == null && !state.finished) {
                    if (percent == null) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        )
                        Text(
                            text = stringResource(R.string.update_progress_percent, percent),
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                state.error?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(if (state.finished || state.error != null) R.string.action_ok else R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
