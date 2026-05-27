package com.example.saleslogdevice.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.model.AppState
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.model.VisitStatus
import com.example.saleslogdevice.domain.usecase.CheckOutResult
import com.example.saleslogdevice.domain.usecase.FinalizeDayUseCase
import com.example.saleslogdevice.domain.util.DateUtils
import com.example.saleslogdevice.ui.HomeUiState
import com.example.saleslogdevice.ui.navigation.Routes
import com.example.saleslogdevice.widget.SalesLogWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    private val _state = MutableStateFlow(HomeUiState(date = DateUtils.today()))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            val today = DateUtils.today()
            val sessionFlow = locator.workSessions.observeByDate(today)
            val settingsFlow = locator.userSettings.observe()

            sessionFlow.flatMapLatest { session ->
                if (session == null) flowOf(Triple<WorkSessionEntity?, List<VisitLogEntity>, Int>(null, emptyList(), 0))
                else combine(
                    locator.visitLogs.observeBySession(session.id),
                    locator.recorderTexts.observeBySession(session.id),
                ) { visits, recs -> Triple(session, visits, recs.size) }
            }.combine(settingsFlow) { triple, settings ->
                val (session, visits, recorderCount) = triple
                val active = visits.firstOrNull { it.status == VisitStatus.CHECKED_IN }
                val appState = when {
                    session == null || session.status == SessionStatus.NOT_STARTED -> AppState.NOT_STARTED
                    session.status == SessionStatus.FINISHED -> AppState.FINISHED
                    active != null -> AppState.VISITING
                    else -> AppState.WORKING
                }
                HomeUiState(
                    date = today,
                    userName = settings.userName,
                    appState = appState,
                    session = session,
                    activeVisit = active,
                    visitCount = visits.size,
                    recorderCount = recorderCount,
                )
            }.collect { newState ->
                _state.update { current ->
                    newState.copy(
                        message = current.message,
                        loading = current.loading,
                        navigateTo = current.navigateTo,
                    )
                }
                SalesLogWidgetProvider.requestUpdate(getApplication())
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeNavigation() = _state.update { it.copy(navigateTo = null) }

    fun startWork() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val locationOk = locator.locationProvider.hasPermission() && locator.locationProvider.isLocationEnabled()
        val location = if (locationOk) locator.locationProvider.getCurrent() else null
        locator.startWorkSession(location)
        _state.update {
            it.copy(
                loading = false,
                message = if (locationOk) "営業を開始しました"
                    else "位置情報を取得できません。端末の位置情報をONにしてください。",
            )
        }
        SalesLogWidgetProvider.requestUpdate(getApplication())
    }

    fun checkOut() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val location = locator.locationProvider.getCurrent()
        val result = locator.checkOut(location)
        val msg = when (result) {
            is CheckOutResult.Success -> "チェックアウトしました"
            CheckOutResult.NoActiveVisit -> "訪問中のログがありません"
        }
        _state.update { it.copy(loading = false, message = msg) }
        SalesLogWidgetProvider.requestUpdate(getApplication())
    }

    /**
     * 営業終了 → 日報生成 → Drive 保存 → Sheets 追記 → 日報画面へ自動遷移、までを1アクションで完了する。
     * Drive/Sheets はMVPではMockのため必ず成功する想定だが、失敗時もローカルには日報が残るので
     * メッセージで結果を伝える。
     */
    fun finishWork() = viewModelScope.launch {
        _state.update { it.copy(loading = true) }
        val location = locator.locationProvider.getCurrent()
        when (val result = locator.finalizeDay(location)) {
            is FinalizeDayUseCase.Result.Success -> {
                val msg = when {
                    result.driveSuccess -> "営業終了で日報を作成しました"
                    else -> "営業を終了し、日報は端末内に保存されました"
                }
                _state.update {
                    it.copy(loading = false, message = msg, navigateTo = Routes.REPORT)
                }
            }
            FinalizeDayUseCase.Result.NoActiveSession -> _state.update {
                it.copy(loading = false, message = "営業中のセッションがありません")
            }
            FinalizeDayUseCase.Result.VisitOngoing -> _state.update {
                it.copy(loading = false, message = "訪問中は営業終了できません。チェックアウトしてください。")
            }
        }
        SalesLogWidgetProvider.requestUpdate(getApplication())
    }
}
