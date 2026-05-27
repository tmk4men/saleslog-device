package com.example.saleslogdevice.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.saleslogdevice.ui.checkin.CheckInScreen
import com.example.saleslogdevice.ui.home.HomeScreen
import com.example.saleslogdevice.ui.navigation.Routes
import com.example.saleslogdevice.ui.report.ReportScreen
import com.example.saleslogdevice.ui.settings.SettingsScreen
import com.example.saleslogdevice.ui.share.ShareReceiveScreen

/** MainActivity から渡される Intent payload。 */
sealed class IntentPayload {
    data object None : IntentPayload()
    data class Share(val title: String?, val text: String) : IntentPayload()
    data class NavigateTo(val route: String) : IntentPayload()
}

data class IncomingShare(val title: String?, val text: String)

/** 外部Intentで指定可能なrouteのallowlist。 */
private val ALLOWED_NAVIGATE_ROUTES = setOf(
    Routes.HOME,
    Routes.CHECK_IN,
    Routes.REPORT,
    Routes.SETTINGS,
)

@Composable
fun AppNavHost(
    payload: IntentPayload,
    onPayloadConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    var currentShare by remember { mutableStateOf<IncomingShare?>(null) }

    LaunchedEffect(payload) {
        when (payload) {
            is IntentPayload.Share -> {
                currentShare = IncomingShare(payload.title, payload.text)
                nav.navigate(Routes.SHARE) {
                    launchSingleTop = true
                }
                onPayloadConsumed()
            }
            is IntentPayload.NavigateTo -> {
                // 外部Intentから渡ってきた route を素通しにしない（不正routeでクラッシュさせない）。
                if (payload.route in ALLOWED_NAVIGATE_ROUTES) {
                    nav.navigate(payload.route) { launchSingleTop = true }
                }
                onPayloadConsumed()
            }
            IntentPayload.None -> Unit
        }
    }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCheckIn = { nav.navigate(Routes.CHECK_IN) },
                onReport = { nav.navigate(Routes.REPORT) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CHECK_IN) {
            CheckInScreen(onClose = { nav.popBackStack() })
        }
        composable(Routes.REPORT) {
            ReportScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SHARE) {
            ShareReceiveScreen(
                incomingTitle = currentShare?.title,
                incomingText = currentShare?.text.orEmpty(),
                onClose = {
                    currentShare = null
                    if (!nav.popBackStack()) {
                        nav.navigate(Routes.HOME) { launchSingleTop = true }
                    }
                },
            )
        }
    }
}
