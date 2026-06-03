# オンデバイス要約（AI Summarization）

構想「オンデバイスAI×Googleスプレッドシート連携」のうち、**文字起こしの要約**部分の実装メモ。

## 現状の構成

```
ReceiveRecorderTextUseCase
   └─ Summarizer (interface)            … domain/ai/Summarizer.kt
        └─ OnDeviceSummarizer            … data/ai/OnDeviceSummarizer.kt（ファサード）
             ├─ primary  : Summarizer?   … 例: GeminiNanoSummarizer（未配線）
             └─ fallback : HeuristicSummarizer  … domain/ai/HeuristicSummarizer.kt
```

- 共有テキストを受信すると `ReceiveRecorderTextUseCase` が **DBトランザクションの外で** 要約を生成し、
  `RecorderTextEntity.summary`（Markdown）に保存する。
- 生成結果は日報Markdown（`GenerateDailyReportUseCase`）と活動履歴シートの「AI要約」列
  （`AppendActivityToSheetsUseCase`）に反映される。
- 現在は `OnDeviceSummarizer(primary = null)` なので、**常にヒューリスティック要約**が使われる。
  外部依存ゼロ・完全オフラインで動作する。

## ヒューリスティック要約の挙動

`HeuristicSummarizer` は生成AIではなく抽出型（重要文の抜き出し）。

- 文を句点・改行・感嘆/疑問符で分割
- 営業文脈キーワード（価格・見積・課題・提案・契約 など）＋位置でスコアリング → 上位3文を要約に採用
- 決定語（決定・合意・受注 など）を含む文 → **要点**
- アクション語（次回・宿題・TODO・送付・フォロー など）を含む行 → **次アクション**

## Gemini Nano（AICore）を本番接続する手順

対応端末: Pixel 8 以降など AICore 搭載機。非対応端末は自動でヒューリスティックにフォールバックする。

1. 依存追加（`app/build.gradle.kts`）:
   ```kotlin
   implementation("com.google.mlkit:genai-summarization:<latest>")
   ```
2. `data/ai/GeminiNanoSummarizer.kt` を実装:
   - Summarization クライアントを生成。
   - `isAvailable()`: AICore のフィーチャー状態を確認。モデル未DLなら download をトリガーし、
     完了まで `false` を返す（その間は自動的にヒューリスティックが使われる）。
   - `summarize()`: SummarizationRequest を投げ、結果を `SummaryResult` に詰める。
     必ず `engine = SummaryResult.Engine.GEMINI_NANO` をセット。
   - 例外時は投げてよい（`OnDeviceSummarizer` が握りつぶしてフォールバックする）。
3. `di/ServiceLocator.kt` を変更:
   ```kotlin
   val summarizer: Summarizer = OnDeviceSummarizer(primary = GeminiNanoSummarizer(appContext))
   ```

これだけで上位レイヤー（UseCase / UI）は無改修のまま、対応端末では Gemini Nano、
非対応端末ではヒューリスティックに切り替わる。

## プロンプト設計の指針（Gemini Nano実装時）

`SummarizationInput` には `transcript` / `clientName` / `title` が入る。日報用途なので:

- 出力は「要約 / 要点 / 次アクション」の3ブロック。
- 主観を足さず、文字起こしに無い事実を作らない。
- 金額・日付・固有名詞は省略しない。
