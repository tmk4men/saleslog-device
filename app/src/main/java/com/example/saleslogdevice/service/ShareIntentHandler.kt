package com.example.saleslogdevice.service

import android.content.Intent

object ShareIntentHandler {

    data class SharedText(val title: String?, val text: String)

    /** ACTION_SEND かつ text/plain なら抽出する。それ以外は null。 */
    fun extract(intent: Intent?): SharedText? {
        intent ?: return null
        if (intent.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return null
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra(Intent.EXTRA_TITLE)?.takeIf { it.isNotBlank() }
        return SharedText(title = title, text = text)
    }
}
