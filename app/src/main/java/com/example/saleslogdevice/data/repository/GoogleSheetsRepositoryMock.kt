package com.example.saleslogdevice.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 追記内容をCSVとしてアプリ内ストレージに残すMock。
 * 内部ファイル: <filesDir>/sheets/<spreadsheetId>_<sheetName>.csv
 */
class GoogleSheetsRepositoryMock(private val context: Context) : GoogleSheetsRepository {

    override suspend fun appendRows(
        spreadsheetId: String,
        sheetName: String,
        rows: List<List<String>>,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "sheets").apply { mkdirs() }
            val file = File(dir, "${spreadsheetId.ifBlank { "local" }}_${sheetName.ifBlank { "sheet" }}.csv")
            file.appendText(rows.joinToString("\n") { row -> row.joinToString(",") { escapeCsv(it) } } + "\n")
            Log.i(TAG, "Mock Sheets appended ${rows.size} row(s) to ${file.absolutePath}")
            Unit
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    companion object {
        private const val TAG = "SheetsMock"
    }
}
