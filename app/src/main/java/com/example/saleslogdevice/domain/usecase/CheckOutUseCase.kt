package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.LatLng
import com.example.saleslogdevice.domain.model.VisitStatus
import com.example.saleslogdevice.domain.util.DateUtils

sealed class CheckOutResult {
    data class Success(val visit: VisitLogEntity) : CheckOutResult()
    data object NoActiveVisit : CheckOutResult()
}

class CheckOutUseCase(
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
) {
    suspend operator fun invoke(location: LatLng?): CheckOutResult {
        val session = sessions.getActiveWorking() ?: return CheckOutResult.NoActiveVisit
        val active = visits.getActive(session.id) ?: return CheckOutResult.NoActiveVisit

        val now = System.currentTimeMillis()
        val updated = active.copy(
            status = VisitStatus.CHECKED_OUT,
            checkOutTime = now,
            checkOutLatitude = location?.latitude,
            checkOutLongitude = location?.longitude,
            durationMinutes = DateUtils.diffMinutes(active.checkInTime, now),
        )
        visits.update(updated)
        return CheckOutResult.Success(updated)
    }
}
