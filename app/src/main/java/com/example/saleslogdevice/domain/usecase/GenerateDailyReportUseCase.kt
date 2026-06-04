package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.RecorderTextEntity
import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.data.repository.RecorderTextRepository
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.util.DateUtils

data class DailyReport(
    val session: WorkSessionEntity,
    val visits: List<VisitLogEntity>,
    val recorderTexts: List<RecorderTextEntity>,
    val markdown: String,
)

class GenerateDailyReportUseCase(
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
    private val recorders: RecorderTextRepository,
) {
    /**
     * セッションIDを指定するか、未指定なら「今日のセッション」を読み込んで日報を生成する。
     * 戻り値が null の場合、対象セッションが存在しない。
     */
    suspend operator fun invoke(sessionId: Long? = null): DailyReport? {
        val session = if (sessionId != null) sessions.getById(sessionId)
            else sessions.getByDate(DateUtils.today())
        session ?: return null
        val visitList = visits.getBySession(session.id)
        val recorderList = recorders.getBySession(session.id)
        return DailyReport(
            session = session,
            visits = visitList,
            recorderTexts = recorderList,
            markdown = buildMarkdown(session, visitList, recorderList),
        )
    }

    private fun buildMarkdown(
        session: WorkSessionEntity,
        visits: List<VisitLogEntity>,
        recorderTexts: List<RecorderTextEntity>,
    ): String = buildString {
        appendLine("# 営業日報")
        appendLine("日付: ${session.date}")
        appendLine("担当: ${session.userName.ifBlank { "（未設定）" }}")
        appendLine()

        appendLine("## 稼働")
        appendLine("開始: ${session.startTime?.let(DateUtils::formatTime) ?: "-"}")
        appendLine("終了: ${session.endTime?.let(DateUtils::formatTime) ?: "-"}")
        appendLine("訪問件数: ${visits.size}件")
        val workMinutes = if (session.startTime != null && session.endTime != null)
            DateUtils.diffMinutes(session.startTime, session.endTime) else null
        if (workMinutes != null) appendLine("総稼働: ${DateUtils.humanDuration(workMinutes)}")
        appendLine()

        if (visits.isEmpty()) {
            appendLine("## 訪問")
            appendLine("（訪問記録なし）")
            appendLine()
        } else {
            visits.forEachIndexed { index, v ->
                appendLine("## 訪問${index + 1}")
                appendLine("訪問先: ${v.clientName}")
                val checkIn = DateUtils.formatTime(v.checkInTime)
                val checkOut = v.checkOutTime?.let(DateUtils::formatTime) ?: "-"
                appendLine("時間: $checkIn - $checkOut")
                if (v.durationMinutes != null) appendLine("滞在: ${DateUtils.humanDuration(v.durationMinutes)}")
                val place = v.checkInAddress?.takeIf(String::isNotBlank)
                    ?: if (v.checkInLatitude != null && v.checkInLongitude != null)
                        "${"%.5f".format(v.checkInLatitude)}, ${"%.5f".format(v.checkInLongitude)}"
                    else null
                if (place != null) appendLine("位置: $place")
                if (v.memo.isNotBlank()) {
                    appendLine()
                    appendLine("メモ: ${v.memo}")
                }
                val linked = recorderTexts.filter { it.visitLogId == v.id }
                appendLine()
                appendLine("### 商談メモ")
                if (linked.isEmpty()) {
                    appendLine("（録音メモなし）")
                } else {
                    linked.forEach { rec ->
                        if (!rec.title.isNullOrBlank()) appendLine("**${rec.title}**")
                        if (!rec.summary.isNullOrBlank()) {
                            appendLine("_AI要約_")
                            appendLine(rec.summary.trim())
                            appendLine()
                        }
                        appendLine(rec.formattedMinutes.trim())
                        appendLine()
                    }
                }
                val nextActions = linked
                    .mapNotNull { TextFormatter.extractNextActions(it.rawText) }
                    .joinToString("\n").ifBlank { null }
                appendLine("### 次アクション")
                appendLine(nextActions ?: "- （未入力）")
                appendLine()
            }
        }

        val unlinked = recorderTexts.filter { it.visitLogId == null }
        if (unlinked.isNotEmpty()) {
            appendLine("## 未紐づけ商談メモ")
            unlinked.forEach { rec ->
                if (!rec.title.isNullOrBlank()) appendLine("**${rec.title}**")
                if (!rec.summary.isNullOrBlank()) {
                    appendLine("_AI要約_")
                    appendLine(rec.summary.trim())
                    appendLine()
                }
                appendLine(rec.formattedMinutes.trim())
                appendLine()
            }
        }
    }.trimEnd() + "\n"
}
