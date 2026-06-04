package com.example.saleslogdevice.ui.share

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareReceiveScreen(
    incomingTitle: String?,
    incomingText: String,
    incomingAudioUri: Uri?,
    onClose: () -> Unit,
    viewModel: ShareReceiveViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(incomingText) {
        if (incomingText.isNotBlank()) viewModel.setIncoming(incomingTitle, incomingText)
    }
    LaunchedEffect(incomingAudioUri) {
        incomingAudioUri?.let { viewModel.setIncomingAudio(incomingTitle, it) }
    }

    LaunchedEffect(state.finished) { if (state.finished) onClose() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isAudio) "録音を取り込み" else "商談メモを取り込み") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "取り込みをキャンセル")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("紐づけ先", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (state.visits.isEmpty()) {
                Text(
                    "今日はまだ訪問がありません。未紐づけ商談メモとして保存します。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        state.visits.forEach { v ->
                            VisitChoiceRow(
                                label = v.clientName,
                                statusLabel = if (v.checkedOut) "終了" else "訪問中",
                                selected = state.selectedVisitId == v.id,
                                onClick = { viewModel.selectVisit(v.id) },
                            )
                        }
                        VisitChoiceRow(
                            label = "紐づけない（未紐づけメモ）",
                            statusLabel = null,
                            selected = state.selectedVisitId == null,
                            onClick = { viewModel.selectVisit(null) },
                        )
                    }
                }
            }
            if (!state.title.isNullOrBlank()) {
                Text("タイトル: ${state.title}", fontSize = 14.sp)
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.previewText.ifBlank { "（本文なし）" },
                    modifier = Modifier
                        .padding(12.dp)
                        .heightIn(min = 120.dp),
                    fontSize = 13.sp,
                )
            }
            Button(
                onClick = viewModel::confirm,
                enabled = !state.submitting && state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(if (state.isAudio) "取り込み中…" else "要約中…", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text(if (state.isAudio) "この録音を取り込む" else "この内容で取り込む", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VisitChoiceRow(
    label: String,
    statusLabel: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 行全体を selectable にしているので RadioButton 自体はクリック対象にしない（二重操作を防ぐ）。
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp)
        if (statusLabel != null) {
            Text(
                statusLabel,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
