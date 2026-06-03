package com.example.saleslogdevice.domain.ai

/**
 * 商談の文字起こしテキストを「営業日報向け」に要約するための抽象。
 *
 * 構想（オンデバイスAI×Googleスプレッドシート連携）では Pixel 端末上の Gemini Nano を
 * 用いて API コストをゼロにすることを目指す。実体は差し替え可能にしておき、
 *  - 端末が Gemini Nano（AICore / ML Kit GenAI）に対応していればそれを使う
 *  - 非対応端末では外部依存ゼロのヒューリスティック要約にフォールバックする
 * という二段構えを [com.example.saleslogdevice.data.ai.OnDeviceSummarizer] で実現する。
 */
interface Summarizer {

    /** この実装が現在の端末で実際に推論可能か（モデルDL済み・対応HWか）。 */
    suspend fun isAvailable(): Boolean

    /**
     * 文字起こしテキストを要約する。
     * 失敗しても例外は投げず、最低限の結果（フォールバック）を返すこと。
     */
    suspend fun summarize(input: SummarizationInput): SummaryResult
}

/**
 * @param transcript 文字起こし全文（必須）
 * @param clientName 訪問先名。要約の文脈付けに使う（任意）
 * @param title Recorder側のタイトル（任意）
 */
data class SummarizationInput(
    val transcript: String,
    val clientName: String? = null,
    val title: String? = null,
)

/**
 * @param summary 2〜4文程度の要約本文
 * @param keyPoints 要点（決定事項・重要トピック）の箇条書き
 * @param nextActions 次アクションの箇条書き
 * @param engine どのエンジンで生成したか（ログ・UI表示用）
 */
data class SummaryResult(
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val nextActions: List<String> = emptyList(),
    val engine: Engine = Engine.HEURISTIC,
) {
    enum class Engine { GEMINI_NANO, HEURISTIC }

    /** 日報・スプレッドシートに載せる単一テキスト表現。 */
    fun toMarkdown(): String = buildString {
        if (summary.isNotBlank()) appendLine(summary.trim())
        if (keyPoints.isNotEmpty()) {
            appendLine()
            appendLine("**要点**")
            keyPoints.forEach { appendLine("- $it") }
        }
        if (nextActions.isNotEmpty()) {
            appendLine()
            appendLine("**次アクション**")
            nextActions.forEach { appendLine("- $it") }
        }
    }.trim()

    /** スプレッドシート1セル向けの短い1〜2文サマリ。 */
    fun toCellText(): String = summary.replace("\n", " ").trim()
}
