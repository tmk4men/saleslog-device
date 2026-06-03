package com.example.saleslogdevice.data.ai

import com.example.saleslogdevice.domain.ai.SummarizationInput
import com.example.saleslogdevice.domain.ai.SummaryResult
import com.example.saleslogdevice.domain.ai.Summarizer

/**
 * Pixel 端末のオンデバイス Gemini Nano（AICore / ML Kit GenAI Summarization）による要約の
 * 差し込み口。**現状はプレースホルダ**で、常に [isAvailable] = false を返すため
 * [OnDeviceSummarizer] は自動的にヒューリスティックへフォールバックする。
 *
 * 本実装する際の手順（docs/ai-summarization.md に詳細）:
 *  1. app/build.gradle.kts に依存を追加:
 *       implementation("com.google.mlkit:genai-summarization:<latest>")
 *  2. Summarization クライアントを生成し、featureStatus で AICore モデルの DL 状態を確認。
 *     未DLなら downloadFeature をトリガーし、完了するまで isAvailable=false を返す。
 *  3. summarize() で SummarizationRequest を投げ、結果を SummaryResult に詰め替える。
 *     `engine = SummaryResult.Engine.GEMINI_NANO` を必ずセットする。
 *  4. ServiceLocator で OnDeviceSummarizer(primary = GeminiNanoSummarizer(appContext)) に変更。
 *
 * 対応端末は Pixel 8 以降など AICore 搭載機に限られるため、必ず isAvailable() で実機判定し、
 * 非対応端末ではフォールバックさせること。
 */
class GeminiNanoSummarizer : Summarizer {

    override suspend fun isAvailable(): Boolean = false

    override suspend fun summarize(input: SummarizationInput): SummaryResult {
        // 未実装。OnDeviceSummarizer が isAvailable()=false を見てここを呼ばない想定。
        throw UnsupportedOperationException("GeminiNanoSummarizer is not wired yet")
    }
}
