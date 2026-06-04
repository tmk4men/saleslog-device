package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.SyncLogEntity
import com.example.saleslogdevice.data.repository.GoogleSheetsRepository
import com.example.saleslogdevice.data.repository.RecorderTextRepository
import com.example.saleslogdevice.data.repository.SyncLogRepository
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.SyncStatus
import com.example.saleslogdevice.domain.model.SyncTarget
import com.example.saleslogdevice.domain.util.DateUtils

data class SheetsSyncResult(val isSuccess: Boolean, val errorMessage: String? = null)

class AppendActivityToSheetsUseCase(
    private val sheets: GoogleSheetsRepository,
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
    private val recorders: RecorderTextRepository,
    private val syncLogs: SyncLogRepository,
) {
    /**
     * 活動履歴を端末内CSV（<filesDir>/sheets/活動履歴.csv）に追記する。
     * 列構成:
     * 日付, 担当者, 営業開始, 営業終了, 訪問先, チェックイン, チェックアウト,
     * 滞在(分), 住所, 録音メモ有無, AI要約, 作成日時
     */
    suspend operator fun invoke(sessionId: Long): SheetsSyncResult {
        val session = sessions.getById(sessionId)
            ?: return SheetsSyncResult(false, "session not found")
        val visitList = visits.getBySession(sessionId)

        val rows: List<List<String>> = if (visitList.isEmpty()) {
            listOf(buildRow(session, null, null, false))
        } else {
            visitList.map { v ->
                val recs = recorders.getByVisit(v.id)
                buildRow(session, v, v.durationMinutes, recs.isNotEmpty())
            }
        }

        val result = sheets.appendRows(spreadsheetId = "", sheetName = CSV_NAME, header = COLUMNS, rows = rows)
        return result.fold(
            onSuccess = {
                sessions.update(session.copy(syncedToSheets = true))
                syncLogs.insert(SyncLogEntity(
                    targetType = SyncTarget.SHEETS,
                    targetId = session.id,
                    status = SyncStatus.SUCCESS,
                ))
                SheetsSyncResult(true)
            },
            onFailure = { ex ->
                syncLogs.insert(SyncLogEntity(
                    targetType = SyncTarget.SHEETS,
                    targetId = session.id,
                    status = SyncStatus.FAILED,
                    errorMessage = ex.message,
                ))
                SheetsSyncResult(false, ex.message)
            }
        )
    }

    private fun buildRow(
        session: com.example.saleslogdevice.data.local.entity.WorkSessionEntity,
        visit: com.example.saleslogdevice.data.local.entity.VisitLogEntity?,
        durationMinutes: Int?,
        hasMemo: Boolean,
    ): List<String> = listOf(
        session.date,
        session.userName,
        session.startTime?.let(DateUtils::formatTime).orEmpty(),
        session.endTime?.let(DateUtils::formatTime).orEmpty(),
        visit?.clientName.orEmpty(),
        visit?.checkInTime?.let(DateUtils::formatTime).orEmpty(),
        visit?.checkOutTime?.let(DateUtils::formatTime).orEmpty(),
        durationMinutes?.toString().orEmpty(),
        visit?.let { v ->
            v.checkInAddress?.takeIf(String::isNotBlank)
                ?: if (v.checkInLatitude != null && v.checkInLongitude != null)
                    "${"%.5f".format(v.checkInLatitude)}, ${"%.5f".format(v.checkInLongitude)}"
                else null
        }.orEmpty(),
        if (hasMemo) "あり" else "なし",
        DateUtils.formatDateTime(System.currentTimeMillis()),
    )

    companion object {
        /** 端末内CSVのファイル名（拡張子なし）。共有/書き出し時のファイル名にもなる。 */
        const val CSV_NAME = "活動履歴"

        /** CSVの列見出し。buildRow の並びと必ず一致させること。 */
        val COLUMNS = listOf(
            "日付", "担当者", "営業開始", "営業終了", "訪問先", "チェックイン", "チェックアウト",
            "滞在(分)", "住所", "録音メモ有無", "作成日時",
        )
    }
}
