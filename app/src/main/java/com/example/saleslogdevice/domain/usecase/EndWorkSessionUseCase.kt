package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.LatLng
import com.example.saleslogdevice.domain.model.SessionStatus

sealed class EndWorkResult {
    data class Success(val session: WorkSessionEntity) : EndWorkResult()
    data object NoActiveSession : EndWorkResult()
    data object VisitOngoing : EndWorkResult()
}

class EndWorkSessionUseCase(
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
    private val transactor: Transactor,
) {
    suspend operator fun invoke(location: LatLng?): EndWorkResult = transactor.run {
        val session = sessions.getActiveWorking() ?: return@run EndWorkResult.NoActiveSession

        val active = visits.getActive(session.id)
        if (active != null) return@run EndWorkResult.VisitOngoing

        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = SessionStatus.FINISHED,
            endTime = now,
            endLatitude = location?.latitude,
            endLongitude = location?.longitude,
        )
        sessions.update(updated)
        EndWorkResult.Success(updated)
    }
}
