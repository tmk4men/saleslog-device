package com.example.saleslogdevice.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.model.VisitStatus
import com.example.saleslogdevice.domain.usecase.ReceiveResult
import com.example.saleslogdevice.widget.SalesLogWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 共有画面で選べる「紐づけ先の訪問」候補。 */
data class VisitOption(
    val id: Long,
    val clientName: String,
    /** チェックアウト済み（訪問中ではない）か。一覧で「訪問中/終了」を出し分けるために持つ。 */
    val checkedOut: Boolean,
)

data class ShareReceiveUiState(
    val title: String? = null,
    val previewText: String = "",
    /** 今日のセッションの訪問一覧。空ならまだ1件もチェックインしていない。 */
    val visits: List<VisitOption> = emptyList(),
    /** 紐づけ先。null は「紐づけない（未紐づけメモ）」。 */
    val selectedVisitId: Long? = null,
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
            val visits = session?.let { locator.visitLogs.getBySession(it.id) }.orEmpty()
            val options = visits.map {
                VisitOption(
                    id = it.id,
                    clientName = it.clientName,
                    checkedOut = it.status == VisitStatus.CHECKED_OUT,
                )
            }
            // 既定の紐づけ先: 訪問中があればそれ、無ければ直近(最後)の訪問。どちらも無ければ未紐づけ(null)。
            // これにより「録音→停止→チェックアウト→共有」でも直近の訪問が既定で選ばれ、取りこぼしを防ぐ。
            val active = visits.firstOrNull { it.status == VisitStatus.CHECKED_IN }
            val default = active?.id ?: visits.lastOrNull()?.id
            _state.update {
                it.copy(
                    title = title,
                    previewText = text,
                    visits = options,
                    selectedVisitId = default,
                )
            }
        }
    }

    /** 紐づけ先を切り替える。null で「紐づけない」。 */
    fun selectVisit(id: Long?) = _state.update { it.copy(selectedVisitId = id) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun confirm() = viewModelScope.launch {
        _state.update { it.copy(submitting = true) }
        val result = locator.receiveRecorderText(pendingTitle, pendingText, state.value.selectedVisitId)
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
