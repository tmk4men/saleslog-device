package com.example.saleslogdevice.domain.ai

/**
 * 外部依存ゼロ・完全オフラインで動く抽出型（extractive）要約。
 *
 * Gemini Nano 非対応端末でも「とりあえず使える」最低保証ラインを提供する。
 * 文を区切り、営業文脈のキーワードと位置でスコアリングして上位文を要約に採用し、
 * 決定事項・次アクションをキーワードで拾い上げる。生成系AIではないので語尾の言い換え等は
 * 行わず、原文の重要文をそのまま抜き出す。
 */
class HeuristicSummarizer : Summarizer {

    override suspend fun isAvailable(): Boolean = true

    override suspend fun summarize(input: SummarizationInput): SummaryResult {
        val text = input.transcript.trim()
        if (text.isEmpty()) {
            return SummaryResult(summary = "", engine = SummaryResult.Engine.HEURISTIC)
        }

        val sentences = splitSentences(text)
        if (sentences.isEmpty()) {
            return SummaryResult(summary = text.take(200), engine = SummaryResult.Engine.HEURISTIC)
        }

        val keyPoints = sentences
            .filter { s -> DECISION_HINTS.any { s.contains(it) } }
            .map { it.trim() }
            .distinct()
            .take(MAX_KEY_POINTS)

        val nextActions = extractNextActions(text)

        // 要約本文: キーワード+位置スコア上位の文を、原文の出現順で並べる。
        val ranked = sentences.mapIndexed { index, s -> index to score(s, index, sentences.size) }
            .sortedByDescending { it.second }
            .take(MAX_SUMMARY_SENTENCES)
            .map { it.first }
            .sorted()
        val summary = ranked.joinToString("") { sentences[it].trim() }
            .ifBlank { sentences.first().trim() }

        return SummaryResult(
            summary = summary,
            keyPoints = keyPoints,
            nextActions = nextActions,
            engine = SummaryResult.Engine.HEURISTIC,
        )
    }

    private fun score(sentence: String, index: Int, total: Int): Double {
        var score = 0.0
        val s = sentence
        IMPORTANT_HINTS.forEach { if (s.contains(it)) score += 2.0 }
        DECISION_HINTS.forEach { if (s.contains(it)) score += 1.5 }
        ACTION_HINTS.forEach { if (s.contains(it)) score += 1.0 }
        // 先頭付近を軽く優遇（導入の文脈が要約に効きやすい）。
        score += (total - index).toDouble() / total * 0.8
        // 極端に短い/長い文を軽く減点。
        val len = s.length
        if (len < 8) score -= 1.0
        if (len > 120) score -= 0.5
        return score
    }

    companion object {
        private const val MAX_SUMMARY_SENTENCES = 3
        private const val MAX_KEY_POINTS = 5
        private const val MAX_NEXT_ACTIONS = 6

        /** 商談で重要度が高い語。 */
        private val IMPORTANT_HINTS = listOf(
            "価格", "見積", "予算", "金額", "コスト", "課題", "要望", "提案",
            "導入", "契約", "受注", "失注", "競合", "比較", "納期", "決裁", "稟議",
        )

        /** 決定・合意を示す語。 */
        private val DECISION_HINTS = listOf(
            "決定", "決まり", "合意", "確定", "承認", "了承", "OK", "ＯＫ",
            "受注", "発注", "契約", "ことにした", "ことになった", "進める",
        )

        /** 次アクションを示す語。 */
        private val ACTION_HINTS = listOf(
            "次回", "次アクション", "宿題", "TODO", "todo", "Todo", "ToDo",
            "対応", "持ち帰り", "持ち帰る", "確認", "送付", "提出", "連絡",
            "フォロー", "再訪", "見積もり提出", "資料", "ネクスト",
        )

        /** 文分割: 日本語句点・改行・感嘆/疑問符で区切る。区切り文字は文末に残す。 */
        fun splitSentences(text: String): List<String> {
            val normalized = text.replace("\r\n", "\n")
            val result = mutableListOf<String>()
            val sb = StringBuilder()
            for (ch in normalized) {
                when (ch) {
                    '。', '！', '？', '!', '?', '\n' -> {
                        if (ch != '\n') sb.append(ch)
                        val piece = sb.toString().trim()
                        if (piece.isNotEmpty()) result.add(piece)
                        sb.setLength(0)
                    }
                    else -> sb.append(ch)
                }
            }
            val tail = sb.toString().trim()
            if (tail.isNotEmpty()) result.add(tail)
            return result
        }

        /** 次アクション抽出（日報の「次アクション」欄でも再利用される共有ロジック）。 */
        fun extractNextActions(text: String): List<String> {
            return text.lines()
                .map { it.trim().trimStart('-', '・', '*', ' ') }
                .filter { line -> line.isNotEmpty() && ACTION_HINTS.any { line.contains(it) } }
                .distinct()
                .take(MAX_NEXT_ACTIONS)
        }
    }
}
