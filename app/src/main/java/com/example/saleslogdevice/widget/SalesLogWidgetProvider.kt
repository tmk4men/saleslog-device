package com.example.saleslogdevice.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.saleslogdevice.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * AppWidget の本体。AppWidgetManager から呼ばれるため exported=true。
 * 独自アクションは [SalesLogWidgetActionReceiver]（exported=false）に分離している。
 *
 * 描画 (onUpdate) は DB 読み込みを伴うため goAsync() で非同期化し、メインスレッドを塞がない。
 */
class SalesLogWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        val scope = ServiceLocator.get(context).applicationScope
        scope.launch {
            try {
                val snapshot = WidgetRenderer.loadSnapshot(context)
                appWidgetIds.forEach { id ->
                    WidgetRenderer.render(context, appWidgetManager, id, snapshot)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        /** 外部から「とにかく今描き直してくれ」と頼むときに使う。 */
        fun requestUpdate(context: Context) = WidgetRenderer.broadcastUpdate(context)
    }
}
