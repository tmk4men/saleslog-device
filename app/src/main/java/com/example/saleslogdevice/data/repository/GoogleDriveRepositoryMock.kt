package com.example.saleslogdevice.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本番Drive連携の代わりにアプリ内ストレージへ保存するMock。
 * 内部ファイル: <filesDir>/reports/<fileName>
 * 仕様確認 / UI動作確認 / オフライン時の保全用。
 */
class GoogleDriveRepositoryMock(private val context: Context) : GoogleDriveRepository {

    override suspend fun saveReport(
        folderId: String,
        fileName: String,
        markdown: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "reports").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeText(markdown)
            Log.i(TAG, "Mock Drive saved: ${file.absolutePath}")
            "mock://drive/${folderId.ifBlank { "root" }}/$fileName"
        }
    }

    companion object {
        private const val TAG = "DriveMock"
    }
}
