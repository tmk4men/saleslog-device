package com.example.saleslogdevice.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.saleslogdevice.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Widget から発火する独自アクションのみを受ける内部Receiver。
 * `android:exported="false"` で宣言するため、他アプリからの呼び出しは届かない。
 * 受信処理は [goAsync] と applicationScope で行い、BroadcastReceiver の寿命を超えてもDBに書ける。
 *
 * 営業終了は [com.example.saleslogdevice.domain.usecase.FinalizeDayUseCase] に集約してあるため、
 * Home画面からの「営業終了」と Widget からの「営業終了」で挙動が同一になる
 * （日報生成→Drive保存→Sheets追記まで）。
 */
class SalesLogWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        val locator = ServiceLocator.get(context)
        locator.applicationScope.launch {
            try {
                handle(context, action)
            } finally {
                WidgetRenderer.broadcastUpdate(context)
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, action: String) {
        val locator = ServiceLocator.get(context)
        when (action) {
            ACTION_START -> locator.startWorkSession(locator.locationProvider.getCurrent())
            ACTION_CHECKOUT -> locator.checkOut(locator.locationProvider.getCurrent())
            ACTION_FINISH -> locator.finalizeDay(locator.locationProvider.getCurrent())
            ACTION_REFRESH -> Unit // 状態だけ再描画。位置取得もしない。
        }
    }

    companion object {
        const val ACTION_START = "com.example.saleslogdevice.widget.ACTION_START"
        const val ACTION_CHECKOUT = "com.example.saleslogdevice.widget.ACTION_CHECKOUT"
        const val ACTION_FINISH = "com.example.saleslogdevice.widget.ACTION_FINISH"
        const val ACTION_REFRESH = "com.example.saleslogdevice.widget.ACTION_REFRESH"
    }
}
