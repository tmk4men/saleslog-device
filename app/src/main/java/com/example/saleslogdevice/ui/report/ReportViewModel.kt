package com.example.saleslogdevice.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val loading: Boolean = true,
    val markdown: String = "",
    val sessionId: Long? = null,
    val driveUrl: String? = null,
    val syncingDrive: Boolean = false,
    val syncingSheets: Boolean = false,
    val message: String? = null,
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val report = locator.generateDailyReport()
        if (report == null) {
            _state.update { it.copy(loading = false, markdown = "（本日の日報はまだありません）") }
        } else {
            _state.update {
                it.copy(
                    loading = false,
                    markdown = report.markdown,
                    sessionId = report.session.id,
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun saveToDrive() = viewModelScope.launch {
        val sessionId = _state.value.sessionId ?: return@launch
        _state.update { it.copy(syncingDrive = true) }
        val result = locator.syncReportToDrive(sessionId, _state.value.markdown)
        _state.update {
            it.copy(
                syncingDrive = false,
                driveUrl = result.driveUrl ?: it.driveUrl,
                message = if (result.isSuccess) "Driveに保存しました（Mock）"
                    else "Drive保存に失敗しました。日報は端末内に保存されています。",
            )
        }
    }

    fun appendToSheets() = viewModelScope.launch {
        val sessionId = _state.value.sessionId ?: return@launch
        _state.update { it.copy(syncingSheets = true) }
        val result = locator.appendActivityToSheets(sessionId, _state.value.driveUrl)
        _state.update {
            it.copy(
                syncingSheets = false,
                message = if (result.isSuccess) "Sheetsに追記しました（Mock）"
                    else "Sheetsへの追記に失敗しました。あとで再送できます。",
            )
        }
    }
}
