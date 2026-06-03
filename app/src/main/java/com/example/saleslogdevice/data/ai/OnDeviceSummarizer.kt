package com.example.saleslogdevice.data.ai

import android.util.Log
import com.example.saleslogdevice.domain.ai.HeuristicSummarizer
import com.example.saleslogdevice.domain.ai.SummarizationInput
import com.example.saleslogdevice.domain.ai.SummaryResult
import com.example.saleslogdevice.domain.ai.Summarizer

/**
 * 端末上で要約を行うファサード。
 *
 * 優先順位:
 *  1. [primary]（例: Gemini Nano / AICore）が利用可能ならそれで要約する。
 *  2. 失敗 or 非対応なら [fallback]（ヒューリスティック）に切り替える。
 *
 * これにより「Pixel ではオンデバイス Gemini、それ以外でも最低限動く」という構想の
 * 二段構えを、上位レイヤー（UseCase）から一切意識せずに使えるようにする。
 *
 * 現状 [primary] は未配線（null）。Gemini Nano 実装が用意できたら ServiceLocator で
 * `OnDeviceSummarizer(primary = GeminiNanoSummarizer(context))` のように差し込むだけでよい。
 * 連携手順は docs/ai-summarization.md を参照。
 */
class OnDeviceSummarizer(
    private val primary: Summarizer? = null,
    private val fallback: Summarizer = HeuristicSummarizer(),
) : Summarizer {

    override suspend fun isAvailable(): Boolean = true // fallback が常に使えるので true

    override suspend fun summarize(input: SummarizationInput): SummaryResult {
        val engine = primary
        if (engine != null) {
            val ok = runCatching { engine.isAvailable() }.getOrDefault(false)
            if (ok) {
                val result = runCatching { engine.summarize(input) }
                    .onFailure { Log.w(TAG, "primary summarizer failed, fallback to heuristic", it) }
                    .getOrNull()
                if (result != null && result.summary.isNotBlank()) return result
            }
        }
        return fallback.summarize(input)
    }

    companion object {
        private const val TAG = "OnDeviceSummarizer"
    }
}
