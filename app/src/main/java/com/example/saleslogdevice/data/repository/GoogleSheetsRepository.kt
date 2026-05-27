package com.example.saleslogdevice.data.repository

/**
 * 活動履歴をGoogle Sheetsに追記するインターフェース。
 * MVPはMock実装、後で本番Sheets APIに差し替える。
 */
interface GoogleSheetsRepository {

    /**
     * @param spreadsheetId 保存先スプレッドシートID
     * @param sheetName シート名
     * @param rows 1行 = 1訪問の活動履歴。順序は固定（仕様の列構成準拠）。
     */
    suspend fun appendRows(
        spreadsheetId: String,
        sheetName: String,
        rows: List<List<String>>,
    ): Result<Unit>
}
