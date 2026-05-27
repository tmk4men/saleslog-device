package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.LatLng
import com.example.saleslogdevice.domain.model.VisitStatus

sealed class CheckInResult {
    data class Success(val visit: VisitLogEntity) : CheckInResult()
    data object NoActiveSession : CheckInResult()
    data object AlreadyVisiting : CheckInResult()
}

class CheckInUseCase(
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
    private val transactor: Transactor,
) {
    /**
     * 「アクティブセッションあるか」「既に訪問中ではないか」を1トランザクションで判定して挿入する。
     * 連打やWidget/UIからの同時操作でも訪問中ログ重複が起きない。
     */
    suspend operator fun invoke(
        clientName: String,
        memo: String,
        location: LatLng?,
    ): CheckInResult = transactor.run {
        val session = sessions.getActiveWorking() ?: return@run CheckInResult.NoActiveSession
        if (visits.getActive(session.id) != null) return@run CheckInResult.AlreadyVisiting

        val now = System.currentTimeMillis()
        val entity = VisitLogEntity(
            workSessionId = session.id,
            clientName = clientName.ifBlank { "（未入力）" },
            memo = memo,
            status = VisitStatus.CHECKED_IN,
            checkInTime = now,
            checkInLatitude = location?.latitude,
            checkInLongitude = location?.longitude,
        )
        val id = visits.insert(entity)
        CheckInResult.Success(entity.copy(id = id))
    }
}
