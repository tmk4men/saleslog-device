package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.SyncLogEntity
import com.example.saleslogdevice.data.repository.GoogleSheetsRepository
import com.example.saleslogdevice.data.repository.RecorderTextRepository
import com.example.saleslogdevice.data.repository.SyncLogRepository
import com.example.saleslogdevice.data.repository.UserSettingsRepository
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
    private val settings: UserSettingsRepository,
    private val syncLogs: SyncLogRepository,
) {
    /**
     * 列構成:
     * 日付, 担当者, 営業開始, 営業終了, 訪問先, チェックイン, チェックアウト,
     * 滞在(分), 緯度, 経度, 録音メモ有無, AI要約, Drive日報URL, 作成日時
     */
    suspend operator fun invoke(sessionId: Long, driveUrl: String?): SheetsSyncResult {
        val session = sessions.getById(sessionId)
            ?: return SheetsSyncResult(false, "session not found")
        val visitList = visits.getBySession(sessionId)
        val s = settings.get()

        val rows: List<List<String>> = if (visitList.isEmpty()) {
            listOf(buildRow(session, null, null, driveUrl, false, ""))
        } else {
            visitList.map { v ->
                val recs = recorders.getByVisit(v.id)
                val summaryCell = recs
                    .mapNotNull { it.summary?.takeIf(String::isNotBlank) }
                    .joinToString(" / ")
                    .replace("\n", " ")
                    .take(MAX_SUMMARY_CELL_CHARS)
                buildRow(session, v, v.durationMinutes, driveUrl, recs.isNotEmpty(), summaryCell)
            }
        }

        val result = sheets.appendRows(s.spreadsheetId, s.sheetName, rows)
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
        driveUrl: String?,
        hasMemo: Boolean,
        summaryCell: String,
    ): List<String> = listOf(
        session.date,
        session.userName,
        session.startTime?.let(DateUtils::formatTime).orEmpty(),
        session.endTime?.let(DateUtils::formatTime).orEmpty(),
        visit?.clientName.orEmpty(),
        visit?.checkInTime?.let(DateUtils::formatTime).orEmpty(),
        visit?.checkOutTime?.let(DateUtils::formatTime).orEmpty(),
        durationMinutes?.toString().orEmpty(),
        visit?.checkInLatitude?.let { "%.5f".format(it) }.orEmpty(),
        visit?.checkInLongitude?.let { "%.5f".format(it) }.orEmpty(),
        if (hasMemo) "あり" else "なし",
        summaryCell,
        driveUrl.orEmpty(),
        DateUtils.formatDateTime(System.currentTimeMillis()),
    )

    companion object {
        private const val MAX_SUMMARY_CELL_CHARS = 500
    }
}
