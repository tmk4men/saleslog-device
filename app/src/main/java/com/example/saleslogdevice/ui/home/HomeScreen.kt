package com.example.saleslogdevice.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.saleslogdevice.domain.model.AppState
import com.example.saleslogdevice.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCheckIn: () -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(state.navigateTo) {
        state.navigateTo?.let { route ->
            viewModel.consumeNavigation()
            when (route) {
                Routes.REPORT -> onReport()
                Routes.CHECK_IN -> onCheckIn()
                Routes.SETTINGS -> onSettings()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("営業ログ端末") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                },
            )
        },
    ) { padding ->
        Content(
            padding = padding,
            stateLabel = stateLabel(state.appState),
            userName = state.userName,
            date = state.date,
            visitCount = state.visitCount,
            recorderCount = state.recorderCount,
            appState = state.appState,
            onPrimary = {
                when (state.appState) {
                    AppState.NOT_STARTED -> viewModel.startWork()
                    AppState.WORKING -> onCheckIn()
                    AppState.VISITING -> viewModel.checkOut()
                    AppState.FINISHED -> onReport()
                }
            },
            onSecondary = {
                when (state.appState) {
                    AppState.WORKING -> viewModel.finishWork()
                    AppState.VISITING -> viewModel.finishWork() // VisitOngoing メッセージが出る
                    else -> onReport()
                }
            },
            primaryLabel = primaryLabel(state.appState),
            secondaryLabel = secondaryLabel(state.appState),
            onReportPreview = onReport,
        )
    }
}

@Composable
private fun Content(
    padding: PaddingValues,
    stateLabel: String,
    userName: String,
    date: String,
    visitCount: Int,
    recorderCount: Int,
    appState: AppState,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    primaryLabel: String,
    secondaryLabel: String?,
    onReportPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(date, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (userName.isBlank()) "担当者未設定" else userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "状態: $stateLabel",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Stat("訪問件数", "$visitCount 件")
                    Stat("録音メモ", "$recorderCount 件")
                }
            }
        }

        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryButtonColor(appState),
            ),
        ) {
            Text(primaryLabel, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        if (secondaryLabel != null) {
            Button(
                onClick = onSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(secondaryLabel, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onReportPreview) {
                Text("日報プレビューを開く")
            }
        }
        Text(
            text = "営業中だけ訪問ログを残します。位置情報は営業ログ作成のために使われます。",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun primaryButtonColor(state: AppState) = when (state) {
    AppState.VISITING -> MaterialTheme.colorScheme.secondary
    AppState.FINISHED -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

private fun stateLabel(state: AppState): String = when (state) {
    AppState.NOT_STARTED -> "未開始"
    AppState.WORKING -> "営業中"
    AppState.VISITING -> "訪問中"
    AppState.FINISHED -> "営業終了済み"
}

private fun primaryLabel(state: AppState): String = when (state) {
    AppState.NOT_STARTED -> "営業開始"
    AppState.WORKING -> "訪問チェックイン"
    AppState.VISITING -> "チェックアウト"
    AppState.FINISHED -> "日報プレビュー"
}

private fun secondaryLabel(state: AppState): String? = when (state) {
    AppState.WORKING -> "営業終了"
    AppState.VISITING -> null // チェックアウトしてからでないと終了不可
    else -> null
}
