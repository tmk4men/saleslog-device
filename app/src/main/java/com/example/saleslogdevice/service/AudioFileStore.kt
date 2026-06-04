package com.example.saleslogdevice.service

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 共有された音声(content:// Uri)をアプリ内ストレージ <filesDir>/recordings/ へコピーし、
 * 表示名(ファイル名)を解決する。再生は [fileFor] のファイルを FileProvider 経由で開く。
 */
class AudioFileStore(private val context: Context) {

    /** content:// の表示名(ファイル名)。取れなければ null。 */
    fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /** Uriの音声を端末内へコピーして File を返す。失敗時は null。 */
    suspend fun importToLocal(uri: Uri, fileName: String): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            val safe = fileName.ifBlank { "recording.m4a" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val dest = File(dir, "${System.currentTimeMillis()}_$safe")
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (copied) dest else null
        }.getOrNull()
    }
}
