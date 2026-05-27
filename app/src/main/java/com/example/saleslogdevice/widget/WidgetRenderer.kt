package com.example.saleslogdevice.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.saleslogdevice.MainActivity
import com.example.saleslogdevice.R
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.model.AppState
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.model.VisitStatus
import com.example.saleslogdevice.domain.util.DateUtils
import com.example.saleslogdevice.ui.navigation.Routes

/** Widget の描画責務だけを集約。Provider/ActionReceiver どちらからも使う。 */
internal object WidgetRenderer {

    data class Snapshot(
        val userName: String,
        val state: AppState,
        val visitCount: Int,
        val recorderCount: Int,
    )

    suspend fun loadSnapshot(context: Context): Snapshot {
        val locator = ServiceLocator.get(context)
        val today = DateUtils.today()
        val session = locator.workSessions.getByDate(today)
        val settings = locator.userSettings.get()
        val visits = session?.let { locator.visitLogs.getBySession(it.id) }.orEmpty()
        val active = visits.firstOrNull { it.status == VisitStatus.CHECKED_IN }
        val state = when {
            session == null || session.status == SessionStatus.NOT_STARTED -> AppState.NOT_STARTED
            session.status == SessionStatus.FINISHED -> AppState.FINISHED
            active != null -> AppState.VISITING
            else -> AppState.WORKING
        }
        val recorderCount = session?.let { locator.recorderTexts.countBySession(it.id) } ?: 0
        return Snapshot(
            userName = settings.userName,
            state = state,
            visitCount = visits.size,
            recorderCount = recorderCount,
        )
    }

    fun render(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        snapshot: Snapshot,
    ) {
        val views = RemoteViews(context.packageName, R.layout.sales_log_widget)

        views.setTextViewText(
            R.id.widget_status,
            "営業ログ ${DateUtils.formatTime(System.currentTimeMillis())}  " +
                snapshot.userName.ifBlank { "担当者未設定" },
        )
        views.setTextViewText(
            R.id.widget_summary,
            "状態: ${stateLabel(snapshot.state)}  訪問 ${snapshot.visitCount}件 / 録音 ${snapshot.recorderCount}件",
        )

        val primary = ContextCompat.getColor(context, R.color.widget_primary)
        val accent = ContextCompat.getColor(context, R.color.widget_accent)
        val disabled = ContextCompat.getColor(context, R.color.widget_primary_disabled)

        applyAction(
            context, views, R.id.widget_btn_start,
            action = SalesLogWidgetActionReceiver.ACTION_START,
            enabled = snapshot.state == AppState.NOT_STARTED,
            enabledColor = primary, disabledColor = disabled,
        )
        applyCheckInAction(
            context, views, R.id.widget_btn_checkin,
            enabled = snapshot.state == AppState.WORKING,
            enabledColor = primary, disabledColor = disabled,
        )
        applyAction(
            context, views, R.id.widget_btn_checkout,
            action = SalesLogWidgetActionReceiver.ACTION_CHECKOUT,
            enabled = snapshot.state == AppState.VISITING,
            enabledColor = accent, disabledColor = disabled,
        )
        applyAction(
            context, views, R.id.widget_btn_finish,
            action = SalesLogWidgetActionReceiver.ACTION_FINISH,
            enabled = snapshot.state == AppState.WORKING,
            enabledColor = primary, disabledColor = disabled,
        )

        manager.updateAppWidget(widgetId, views)
    }

    private fun applyAction(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        action: String,
        enabled: Boolean,
        enabledColor: Int,
        disabledColor: Int,
    ) {
        views.setInt(viewId, "setBackgroundColor", if (enabled) enabledColor else disabledColor)
        val intent = if (enabled) action else SalesLogWidgetActionReceiver.ACTION_REFRESH
        views.setOnClickPendingIntent(viewId, actionPendingIntent(context, intent))
    }

    private fun applyCheckInAction(
        context: Context,
        views: RemoteViews,
        viewId: Int,
        enabled: Boolean,
        enabledColor: Int,
        disabledColor: Int,
    ) {
        views.setInt(viewId, "setBackgroundColor", if (enabled) enabledColor else disabledColor)
        // チェックインは訪問先名の入力が必要なのでアプリのチェックイン画面を開く。
        val pendingIntent = if (enabled) checkInActivityPendingIntent(context)
            else actionPendingIntent(context, SalesLogWidgetActionReceiver.ACTION_REFRESH)
        views.setOnClickPendingIntent(viewId, pendingIntent)
    }

    private fun actionPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SalesLogWidgetActionReceiver::class.java).apply {
            this.action = action
            `package` = context.packageName
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun checkInActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ROUTE, Routes.CHECK_IN)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_CHECK_IN_ACTIVITY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun broadcastUpdate(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, SalesLogWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val intent = Intent(context, SalesLogWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }

    private fun stateLabel(state: AppState): String = when (state) {
        AppState.NOT_STARTED -> "未開始"
        AppState.WORKING -> "営業中"
        AppState.VISITING -> "訪問中"
        AppState.FINISHED -> "営業終了済み"
    }

    private const val REQUEST_CHECK_IN_ACTIVITY = 1001
}
