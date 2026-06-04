package com.example.saleslogdevice.data.repository

/**
 * 活動履歴を記録するインターフェース。
 * 現状は端末内CSV(Mock)に蓄積する。将来本番Sheets APIに差し替え可能。
 */
interface GoogleSheetsRepository {

    /**
     * @param spreadsheetId 保存先スプレッドシートID（CSVではファイル名の一部）
     * @param sheetName シート名（CSVではファイル名の一部）
     * @param header 列見出し。記録先が空（新規）の場合のみ先頭行として書き込む。
     * @param rows 1行 = 1訪問の活動履歴。順序は固定（仕様の列構成準拠）。
     */
    suspend fun appendRows(
        spreadsheetId: String,
        sheetName: String,
        header: List<String>,
        rows: List<List<String>>,
    ): Result<Unit>
}
