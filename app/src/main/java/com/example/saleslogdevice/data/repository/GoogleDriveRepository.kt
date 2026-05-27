package com.example.saleslogdevice.data.repository

/**
 * 日報MarkdownをGoogle Driveに保存するインターフェース。
 * MVPではMock実装で動作確認し、後で本番Drive APIに差し替える。
 */
interface GoogleDriveRepository {

    /**
     * @param folderId 保存先フォルダID（空なら "My Drive" 直下扱い）
     * @param fileName 例: 2026-05-27_山田太郎_営業日報.md
     * @param markdown 日報本文
     * @return 成功時はファイルのWeb閲覧URL、失敗時はResult.failure
     */
    suspend fun saveReport(
        folderId: String,
        fileName: String,
        markdown: String,
    ): Result<String>
}
