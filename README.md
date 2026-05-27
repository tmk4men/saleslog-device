# 営業ログ端末 (SalesLogDevice) MVP

会社支給Android端末にプリインストールして渡す前提の「営業ログ端末」アプリ。
営業担当が外回り中に最小タップで日報が出来上がる構造を目指す。

## MVPで動くもの

- 営業開始 / 終了（位置情報チェックポイント記録付き）
- 訪問チェックイン / チェックアウト（滞在時間自動計算）
- Pixel Recorder などからの共有テキスト受信 → 訪問ログに自動紐づけ
- 日報 Markdown 生成（簡易整形のみ、AI不使用）
- Drive 保存 / Sheets 追記の Repository インターフェース＋Mock 実装
- ホーム画面ウィジェット（営業開始 / チェックイン / チェックアウト / 営業終了）

外部 API（Google Drive / Sheets）は Mock。`ServiceLocator.driveRepo` / `sheetsRepo`
を本番実装に差し替えれば本番連携になる。Mock は内部ストレージに保存する：

- Drive: `<filesDir>/reports/<yyyy-MM-dd>_<userName>_営業日報.md`
- Sheets: `<filesDir>/sheets/<spreadsheetId>_<sheetName>.csv`

## ディレクトリ構成

```
app/src/main/java/com/example/saleslogdevice
├── SalesLogApplication.kt
├── MainActivity.kt
├── data
│   ├── local            # Room
│   │   ├── AppDatabase.kt
│   │   ├── Converters.kt
│   │   ├── dao/
│   │   └── entity/
│   └── repository       # Repository 群（Drive/Sheets はインターフェース＋Mock）
├── di
│   └── ServiceLocator.kt
├── domain
│   ├── model            # 列挙・DTO
│   ├── usecase          # 営業開始/終了, チェックイン/アウト, 共有受信, 日報生成, Drive/Sheets 同期
│   └── util/DateUtils.kt
├── service
│   ├── LocationProvider.kt
│   └── ShareIntentHandler.kt
├── ui                   # Compose UI / ViewModel
│   ├── home/ checkin/ report/ settings/ share/
│   ├── navigation/Routes.kt
│   ├── theme/Theme.kt
│   └── AppNavHost.kt
└── widget
    └── SalesLogWidgetProvider.kt
```

## ビルド

Android Studio Hedgehog 以降、AGP 8.5+, Kotlin 2.0+, JDK 17 を想定。
`com.google.devtools.ksp` プラグインで Room のコード生成を行う。

```
./gradlew :app:assembleDebug
```

## 共有 Intent

`AndroidManifest.xml` で `MainActivity` が以下を受ける：

- `action=SEND` / `mimeType=text/plain` （Pixel Recorder の文字起こし/要約の共有）

受信した本文は `ShareReceiveScreen` で確認 → 1タップで取り込み。

## ウィジェット

- 4 ボタン: 営業開始 / チェックイン / チェックアウト / 営業終了
- 現在の状態に応じて押せるボタンのみ濃い色で表示
- チェックインだけは訪問先名入力が必要なのでアプリのチェックイン画面に遷移、
  それ以外はバックグラウンドで実行して状態を再描画

## 将来差し替えポイント

- `GoogleDriveRepository` / `GoogleSheetsRepository`: Google Sign-In + Drive/Sheets API 実装
- `LocationProvider`: 営業中のみの定期トラッキング（必要になったときに追加）
- `GenerateDailyReportUseCase`: AI による要約・次アクション抽出
