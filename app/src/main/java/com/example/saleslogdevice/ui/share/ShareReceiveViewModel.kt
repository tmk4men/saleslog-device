package com.example.saleslogdevice.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.usecase.ReceiveResult
import com.example.saleslogdevice.widget.SalesLogWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareReceiveUiState(
    val title: String? = null,
    val previewText: String = "",
    val activeVisitName: String? = null,
    val confirmed: Boolean = false,
    val submitting: Boolean = false,
    val message: String? = null,
    val finished: Boolean = false,
)

class ShareReceiveViewModel(application: Application) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    private val _state = MutableStateFlow(ShareReceiveUiState())
    val state: StateFlow<ShareReceiveUiState> = _state.asStateFlow()

    private var pendingTitle: String? = null
    private var pendingText: String = ""

    fun setIncoming(title: String?, text: String) {
        pendingTitle = title
        pendingText = text
        viewModelScope.launch {
            val session = locator.workSessions.getActiveWorking()
            val active = session?.let { locator.visitLogs.getActive(it.id) }
            _state.update {
                it.copy(
                    title = title,
                    previewText = text,
                    activeVisitName = active?.clientName,
                )
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun confirm() = viewModelScope.launch {
        _state.update { it.copy(submitting = true) }
        val result = locator.receiveRecorderText(pendingTitle, pendingText)
        val msg = when (result) {
            is ReceiveResult.LinkedToVisit -> "訪問ログに紐づけました"
            is ReceiveResult.StoredUnlinked -> "未紐づけ商談メモとして保存しました"
            ReceiveResult.EmptyText -> "共有テキストが空でした。"
            ReceiveResult.NoActiveSession -> "営業開始後に共有テキストを受け取れます。"
        }
        _state.update { it.copy(submitting = false, message = msg, finished = true) }
        SalesLogWidgetProvider.requestUpdate(getApplication())
    }
}
