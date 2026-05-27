package com.example.saleslogdevice.ui

import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.domain.model.AppState

data class HomeUiState(
    val date: String = "",
    val userName: String = "",
    val appState: AppState = AppState.NOT_STARTED,
    val session: WorkSessionEntity? = null,
    val activeVisit: VisitLogEntity? = null,
    val visitCount: Int = 0,
    val recorderCount: Int = 0,
    val message: String? = null,
    val loading: Boolean = false,
    /** 営業終了直後の自動遷移などで、画面側に「次に開く画面」を伝えるための1ショットイベント。 */
    val navigateTo: String? = null,
)
