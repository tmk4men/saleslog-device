package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.data.local.entity.RecorderTextEntity
import com.example.saleslogdevice.data.repository.RecorderTextRepository
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.ai.SummarizationInput
import com.example.saleslogdevice.domain.ai.Summarizer

sealed class ReceiveResult {
    /** 訪問中: 自動で訪問ログに紐づけた */
    data class LinkedToVisit(val entity: RecorderTextEntity, val visitId: Long) : ReceiveResult()
    /** 訪問中ではない: 今日のセッションに未紐づけ商談メモとして保存 */
    data class StoredUnlinked(val entity: RecorderTextEntity) : ReceiveResult()
    data object NoActiveSession : ReceiveResult()
    data object EmptyText : ReceiveResult()
}

class ReceiveRecorderTextUseCase(
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
    private val recorders: RecorderTextRepository,
    private val transactor: Transactor,
    private val summarizer: Summarizer,
) {
    suspend operator fun invoke(title: String?, rawText: String): ReceiveResult {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) return ReceiveResult.EmptyText

        // 要約は端末内推論（Gemini Nano 等）が走り得るため、DBトランザクションの外で先に実行する。
        val summary = runCatching {
            summarizer.summarize(SummarizationInput(transcript = cleaned, title = title)).toMarkdown()
        }.getOrNull()?.ifBlank { null }

        return transactor.run {
            val session = sessions.getActiveWorking() ?: return@run ReceiveResult.NoActiveSession
            val formatted = TextFormatter.formatMinutes(cleaned)
            val active = visits.getActive(session.id)

            val base = RecorderTextEntity(
                workSessionId = session.id,
                visitLogId = active?.id,
                title = title,
                rawText = cleaned,
                formattedMinutes = formatted,
                summary = summary,
            )
            val id = recorders.insert(base)
            val saved = base.copy(id = id)

            if (active != null) {
                visits.update(active.copy(recorderTextId = id))
                ReceiveResult.LinkedToVisit(saved, active.id)
            } else {
                ReceiveResult.StoredUnlinked(saved)
            }
        }
    }
}

/**
 * Recorder共有テキストの簡易整形。AIは使わずヒューリスティックのみ。
 *  - 連続する空行を1行に圧縮
 *  - 既存の見出し（「要約」「決定事項」「次回」「宿題」「TODO」「対応」「ネクスト」等）はそのまま残す
 *  - 何もマーカーが無い場合は先頭に「商談メモ」見出しを付与
 */
internal object TextFormatter {
    private val SECTION_HINTS = listOf(
        "要約", "決定事項", "次回", "宿題", "TODO", "todo", "Todo",
        "対応", "ネクスト", "Next", "next", "次アクション",
    )

    fun formatMinutes(raw: String): String {
        val collapsed = raw.replace(Regex("\\n{3,}"), "\n\n").trim()
        val hasHint = SECTION_HINTS.any { collapsed.contains(it) }
        return if (hasHint) collapsed else "## 商談メモ\n$collapsed"
    }

    /** 「次回」「対応」「宿題」「TODO」を含む行を抜き出して箇条書きにする。無ければ null。 */
    fun extractNextActions(text: String): String? {
        val hints = listOf("次回", "対応", "宿題", "TODO", "todo", "Todo", "ネクスト", "次アクション")
        val matches = text.lines()
            .map { it.trim() }
            .filter { line -> line.isNotEmpty() && hints.any { line.contains(it) } }
            .distinct()
        if (matches.isEmpty()) return null
        return matches.joinToString("\n") { "- $it" }
    }
}
