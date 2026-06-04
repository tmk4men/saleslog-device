package com.example.saleslogdevice.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 活動履歴をCSVとしてアプリ内ストレージに蓄積する実装。
 * 内部ファイル: <filesDir>/sheets/<spreadsheetId>_<sheetName>.csv
 * 新規作成時のみ先頭にヘッダー行を書き込む。共有/書き出しは [fileFor] のファイルを使う。
 */
class GoogleSheetsRepositoryMock(private val context: Context) : GoogleSheetsRepository {

    /** 蓄積先CSVファイル。存在しなくてもパスを返す（共有判定は呼び出し側で exists() する）。 */
    fun fileFor(spreadsheetId: String, sheetName: String): File {
        val dir = File(context.filesDir, "sheets")
        val name = sheetName.ifBlank { "活動履歴" }
        // spreadsheetId が無い（端末内CSV運用）ならシート名だけのきれいなファイル名にする。
        val fileName = if (spreadsheetId.isBlank()) "$name.csv" else "${spreadsheetId}_$name.csv"
        return File(dir, fileName)
    }

    override suspend fun appendRows(
        spreadsheetId: String,
        sheetName: String,
        header: List<String>,
        rows: List<List<String>>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = fileFor(spreadsheetId, sheetName)
            file.parentFile?.mkdirs()
            val sb = StringBuilder()
            if ((!file.exists() || file.length() == 0L) && header.isNotEmpty()) {
                sb.append(toCsvLine(header)).append("\n")
            }
            rows.forEach { row -> sb.append(toCsvLine(row)).append("\n") }
            file.appendText(sb.toString())
            Log.i(TAG, "CSV appended ${rows.size} row(s) to ${file.absolutePath}")
            Unit
        }
    }

    private fun toCsvLine(values: List<String>): String =
        values.joinToString(",") { escapeCsv(it) }

    private fun escapeCsv(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    companion object {
        private const val TAG = "SheetsMock"
    }
}
