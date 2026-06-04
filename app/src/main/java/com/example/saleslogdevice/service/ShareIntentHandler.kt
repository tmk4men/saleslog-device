package com.example.saleslogdevice.service

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

object ShareIntentHandler {

    data class SharedText(val title: String?, val text: String)
    data class SharedAudio(val title: String?, val uri: Uri)

    /** ACTION_SEND かつ text/plain なら抽出する。それ以外は null。 */
    fun extract(intent: Intent?): SharedText? {
        intent ?: return null
        if (intent.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return null
        val title = sharedTitle(intent)
        return SharedText(title = title, text = text)
    }

    /** ACTION_SEND かつ MIME が audio で始まるなら音声Uriを抽出する。それ以外は null。 */
    fun extractAudio(intent: Intent?): SharedAudio? {
        intent ?: return null
        if (intent.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("audio/") != true) return null
        val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            ?: return null
        return SharedAudio(title = sharedTitle(intent), uri = uri)
    }

    private fun sharedTitle(intent: Intent): String? =
        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)?.takeIf { it.isNotBlank() }
}
