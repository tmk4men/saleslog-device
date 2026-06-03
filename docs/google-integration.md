# Google スプレッドシート / ドライブ 本番連携

構想の最終出力（活動履歴をスプレッドシートに集約し、日報をドライブに保存）を
**本番のGoogle API**に接続するためのメモ。現状は Mock 実装（端末内CSV / ファイル保存）。

## 現状

| 役割 | インターフェース | 現在の実装 | 保存先 |
|---|---|---|---|
| 活動履歴の追記 | `GoogleSheetsRepository` | `GoogleSheetsRepositoryMock` | `<filesDir>/sheets/*.csv` |
| 日報の保存 | `GoogleDriveRepository` | `GoogleDriveRepositoryMock` | `<filesDir>/reports/*` |

差し替えポイントは `di/ServiceLocator.kt` の `driveRepo` / `sheetsRepo` の一行のみ。

## 本番化に必要な準備（ユーザー側作業）

1. **Google Cloud プロジェクト**を作成。
2. **API を有効化**: Google Sheets API / Google Drive API。
3. **OAuth 同意画面**を構成（テスト/本番）、スコープ:
   - `https://www.googleapis.com/auth/spreadsheets`
   - `https://www.googleapis.com/auth/drive.file`
4. **Android用 OAuth クライアントID** を作成（パッケージ名 `app.saleslog` + 署名証明書のSHA-1）。

## 実装方針（2案）

### 案A: Android から直接 Google API（推奨・サーバ不要）
- サインイン: **Credential Manager + Google ID**（旧 Google Sign-In は非推奨化）でアカウント取得。
- 認可: 上記スコープのアクセストークンを取得（`AuthorizationClient` / `GoogleAccountCredential`）。
- 送信:
  - Sheets: `spreadsheets.values.append`（`valueInputOption=USER_ENTERED`）。
    既存の `appendRows(spreadsheetId, sheetName, rows)` の rows をそのまま `values` に渡す。
  - Drive: `files.create`（multipart, mimeType `text/markdown`）でフォルダ `folderId` 配下に保存し、
    `webViewLink` を返す（`saveReport` の戻り値 URL に使う）。
- 列構成（Sheets, 現行 `AppendActivityToSheetsUseCase` 準拠）:
  `日付, 担当者, 営業開始, 営業終了, 訪問先, チェックイン, チェックアウト, 滞在(分), 緯度, 経度, 録音メモ有無, AI要約, Drive日報URL, 作成日時`

### 案B: Apps Script Web App 経由（最も手軽・OAuth簡略）
- スプレッドシートに紐づく **Apps Script を Web App としてデプロイ**（doPost で行追記＋Driveファイル作成）。
- アプリ側は `INTERNET` 権限だけで、デプロイURLへ POST するだけ。OAuthクライアント不要。
- 欠点: URLの秘匿管理が必要。社内利用ならこちらが圧倒的に早い。

## 差し替え例

```kotlin
// ServiceLocator.kt
val sheetsRepo: GoogleSheetsRepository = GoogleSheetsApiRepository(appContext, authProvider)
val driveRepo:  GoogleDriveRepository  = GoogleDriveApiRepository(appContext, authProvider)
```

`Result` を返す契約・メソッドシグネチャは現行のまま維持すれば、UseCase 以上は無改修。

## 設定値の入力

`UserSettingsEntity` に `spreadsheetId` / `sheetName` / `driveFolderId` が既にある。
設定画面（`ui/settings`）から入力 → これらを各 Repository が参照する。
