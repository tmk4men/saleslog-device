package com.example.saleslogdevice.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    onClose: () -> Unit,
    viewModel: ShareReceiveViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(incomingText) {
        if (incomingText.isNotBlank()) viewModel.setIncoming(incomingTitle, incomingText)
    }

    LaunchedEffect(state.finished) { if (state.finished) onClose() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("商談メモを取り込み") }) },
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
            if (state.activeVisitName != null) {
                Text(
                    "この訪問に紐づけます: ${state.activeVisitName}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "訪問中ではありません。未紐づけ商談メモとして保存します。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
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
                enabled = !state.submitting && state.previewText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp),
            ) {
                Text("この内容で取り込む", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
