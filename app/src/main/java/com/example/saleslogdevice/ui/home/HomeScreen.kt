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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("FieldLog", fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Content(
            padding = padding,
            userName = state.userName,
            date = state.date,
            visitCount = state.visitCount,
            recorderCount = state.recorderCount,
            appState = state.appState,
            loading = state.loading,
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
            onReportPreview = onReport,
        )
    }
}

@Composable
private fun Content(
    padding: PaddingValues,
    userName: String,
    date: String,
    visitCount: Int,
    recorderCount: Int,
    appState: AppState,
    loading: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onReportPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeroCard(
            userName = userName,
            date = date,
            appState = appState,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Place,
                label = "訪問件数",
                value = "$visitCount",
                unit = "件",
                tint = MaterialTheme.colorScheme.primary,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Mic,
                label = "録音メモ",
                value = "$recorderCount",
                unit = "件",
                tint = MaterialTheme.colorScheme.secondary,
            )
        }

        PrimaryAction(appState = appState, loading = loading, onClick = onPrimary)

        secondaryLabel(appState)?.let { label ->
            FilledTonalButton(
                onClick = onSecondary,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("処理中…", style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        ReportLink(onClick = onReportPreview)

        Spacer(Modifier.height(4.dp))
        PrivacyNote()
    }
}

@Composable
private fun HeroCard(userName: String, date: String, appState: AppState) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(gradient)
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = greeting(appState, userName),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subMessage(appState),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(18.dp))
            DayProgress(filledSteps = filledSteps(appState))
        }
    }
}

private val ProgressLabels = listOf("開始", "訪問", "記録", "日報")

@Composable
private fun DayProgress(filledSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressLabels.forEachIndexed { index, label ->
            val done = index < filledSteps
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (done) Color.White else Color.White.copy(alpha = 0.30f),
                        ),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (done) Color.White else Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    tint: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.size(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(appState: AppState, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = primaryButtonColor(appState),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = "処理中…",
                style = MaterialTheme.typography.headlineSmall,
            )
        } else {
            Icon(primaryIcon(appState), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(12.dp))
            Text(
                text = primaryLabel(appState),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportLink(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = "日報プレビューを開く",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) { Text("開く") }
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = "位置情報は営業中だけ、日報づくりのために使います。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun primaryButtonColor(state: AppState) = when (state) {
    AppState.VISITING -> MaterialTheme.colorScheme.secondary
    AppState.FINISHED -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

private fun primaryIcon(state: AppState): ImageVector = when (state) {
    AppState.NOT_STARTED -> Icons.Filled.PlayArrow
    AppState.WORKING -> Icons.Filled.Place
    AppState.VISITING -> Icons.Filled.Logout
    AppState.FINISHED -> Icons.Filled.Description
}

private fun greeting(state: AppState, userName: String): String {
    val name = if (userName.isBlank()) "" else "${userName}さん、"
    return when (state) {
        AppState.NOT_STARTED -> "${name}いってらっしゃい"
        AppState.WORKING -> "${name}営業中です"
        AppState.VISITING -> "訪問中です"
        AppState.FINISHED -> "${name}おつかれさまでした"
    }
}

private fun subMessage(state: AppState): String = when (state) {
    AppState.NOT_STARTED -> "準備ができたら営業を開始しましょう"
    AppState.WORKING -> "訪問先に着いたら、ワンタップでチェックイン"
    AppState.VISITING -> "終わったらチェックアウトするだけ"
    AppState.FINISHED -> "日報は自動でできています"
}

private fun filledSteps(state: AppState): Int = when (state) {
    AppState.NOT_STARTED -> 0
    AppState.WORKING -> 1
    AppState.VISITING -> 2
    AppState.FINISHED -> 4
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
