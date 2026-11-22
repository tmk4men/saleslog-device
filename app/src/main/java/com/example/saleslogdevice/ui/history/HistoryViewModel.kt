package com.example.saleslogdevice.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.model.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 過去の日報一覧に表示する1日分のサマリ。 */
data class HistoryItem(
    val sessionId: Long,
    val date: String,
    val finished: Boolean,
    val startTime: Long?,
    val endTime: Long?,
    val visitCount: Int,
)

data class HistoryUiState(
    val loading: Boolean = true,
    val items: List<HistoryItem> = emptyList(),
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val items = locator.workSessions.getRecent().map { session ->
            HistoryItem(
                sessionId = session.id,
                date = session.date,
                finished = session.status == SessionStatus.FINISHED,
                startTime = session.startTime,
                endTime = session.endTime,
                visitCount = locator.visitLogs.countBySession(session.id),
            )
        }
        _state.update { it.copy(loading = false, items = items) }
    }
}
