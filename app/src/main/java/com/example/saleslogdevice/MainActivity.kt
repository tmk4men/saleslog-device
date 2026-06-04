package com.example.saleslogdevice

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.saleslogdevice.service.ShareIntentHandler
import com.example.saleslogdevice.ui.AppNavHost
import com.example.saleslogdevice.ui.IntentPayload
import com.example.saleslogdevice.ui.navigation.Routes
import com.example.saleslogdevice.ui.theme.SalesLogTheme
import com.example.saleslogdevice.widget.SalesLogWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val intentFlow = MutableStateFlow<IntentPayload>(IntentPayload.None)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentFlow.value = parsePayload(intent)

        setContent {
            SalesLogTheme {
                val payload by intentFlow.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { /* 結果は使わない: UseCase 側で改めて権限を確認 */ }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(requiredPermissions())
                }

                AppNavHost(
                    payload = payload,
                    onPayloadConsumed = { intentFlow.value = IntentPayload.None },
                )
            }
        }
        SalesLogWidgetProvider.requestUpdate(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // singleTask で前面に戻ってきた時、共有Intent / Widget からのルート指定 を反映する。
        intentFlow.value = parsePayload(intent)
    }

    private fun parsePayload(intent: Intent?): IntentPayload {
        if (intent == null) return IntentPayload.None
        ShareIntentHandler.extract(intent)?.let {
            return IntentPayload.Share(title = it.title, text = it.text)
        }
        ShareIntentHandler.extractAudio(intent)?.let {
            return IntentPayload.ShareAudio(title = it.title, uri = it.uri)
        }
        val route = intent.getStringExtra(EXTRA_ROUTE)
        if (!route.isNullOrBlank()) return IntentPayload.NavigateTo(route)
        return IntentPayload.None
    }

    private fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    companion object {
        /** Widget からの起動などで遷移したい先を指定する。値は [Routes] を使う。 */
        const val EXTRA_ROUTE = "com.example.saleslogdevice.extra.ROUTE"
    }
}
