package com.example.saleslogdevice.ui.report

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.csvSharePath) {
        val path = state.csvSharePath ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", File(path),
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "営業活動履歴CSV")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "CSVを共有"))
        viewModel.consumeShare()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("日報プレビュー") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = viewModel::saveToDrive,
                    enabled = !state.syncingDrive && state.sessionId != null,
                    modifier = Modifier.weight(1f),
                ) { Text("Drive保存") }
                OutlinedButton(
                    onClick = viewModel::appendToSheets,
                    enabled = !state.syncingSheets && state.sessionId != null,
                    modifier = Modifier.weight(1f),
                ) { Text("CSVに記録") }
            }
            OutlinedButton(
                onClick = viewModel::shareCsv,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("CSVを共有 / 書き出し") }
            if (state.driveUrl != null) {
                Text("Drive: ${state.driveUrl}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = state.markdown,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}
