package com.example.saleslogdevice.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.util.DateUtils
import com.example.saleslogdevice.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 日報画面に表示する録音1件。再生は path を FileProvider 経由で開く。 */
data class RecordingItem(val id: Long, val label: String, val path: String)

data class ReportUiState(
    val loading: Boolean = true,
    val markdown: String = "",
    val sessionId: Long? = null,
    val recording: Boolean = false,
    val message: String? = null,
    /** 共有すべきCSVの絶対パス。画面側が共有Intentを起動したら consumeShare() でクリアする。 */
    val csvSharePath: String? = null,
    /** このセッションに紐づく録音一覧。 */
    val recordings: List<RecordingItem> = emptyList(),
    /** 今日の終了済みセッションを表示中で、「営業に戻る」ボタンを出せるか。 */
    val canResume: Boolean = false,
    /** 営業に戻る処理が完了したら true。画面側がホームへ戻る合図に使う。 */
    val resumed: Boolean = false,
)

class ReportViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    /** 過去の日報を開いたときの sessionId。未指定（< 0）なら今日の日報を表示する。 */
    private val sessionIdArg: Long? =
        savedStateHandle.get<Long>(Routes.REPORT_ARG_SESSION_ID)?.takeIf { it >= 0 }

    private val _state = MutableStateFlow(ReportUiState())
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val report = locator.generateDailyReport(sessionIdArg)
        if (report == null) {
            _state.update { it.copy(loading = false, markdown = "（この日の日報はまだありません）") }
        } else {
            val visitNames = report.visits.associate { it.id to it.clientName }
            val recordings = locator.audioRecordings.getBySession(report.session.id).map { rec ->
                val visitName = rec.visitLogId?.let { visitNames[it] }
                val base = rec.title?.takeIf { it.isNotBlank() } ?: "録音"
                RecordingItem(
                    id = rec.id,
                    label = if (visitName != null) "$base（$visitName）" else base,
                    path = rec.filePath,
                )
            }
            _state.update {
                it.copy(
                    loading = false,
                    markdown = report.markdown,
                    sessionId = report.session.id,
                    recordings = recordings,
                    canResume = report.session.date == DateUtils.today() &&
                        report.session.status == SessionStatus.FINISHED,
                )
            }
        }
    }

    /** 今日の終了済みセッションを WORKING に戻し、もう一度営業に戻る。 */
    fun resumeWork() = viewModelScope.launch {
        locator.resumeWorkSession()
        _state.update { it.copy(resumed = true) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun consumeShare() = _state.update { it.copy(csvSharePath = null) }

    /** 蓄積済み活動CSVを共有する。記録がまだ無ければメッセージのみ。 */
    fun shareCsv() = viewModelScope.launch {
        val file = locator.activityCsvFile()
        if (file == null) {
            _state.update { it.copy(message = "まだ記録されたCSVがありません。先に「CSVに記録」してください。") }
        } else {
            _state.update { it.copy(csvSharePath = file.absolutePath) }
        }
    }

    fun appendToSheets() = viewModelScope.launch {
        val sessionId = _state.value.sessionId ?: return@launch
        _state.update { it.copy(recording = true) }
        val result = locator.appendActivityToSheets(sessionId)
        _state.update {
            it.copy(
                recording = false,
                message = if (result.isSuccess) "CSVに記録しました"
                    else "CSVへの記録に失敗しました。あとで再実行できます。",
            )
        }
    }
}
