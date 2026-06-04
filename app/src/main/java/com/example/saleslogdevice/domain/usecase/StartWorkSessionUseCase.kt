package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.data.repository.UserSettingsRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.LatLng
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.util.DateUtils

class StartWorkSessionUseCase(
    private val sessions: WorkSessionRepository,
    private val settings: UserSettingsRepository,
    private val transactor: Transactor,
) {
    /**
     * 今日のセッションが NOT_STARTED なら更新、無ければ新規作成して WORKING にする。
     * すでに WORKING / FINISHED ならそのまま返す（再開はMVPでは扱わない）。
     * date には unique index があるため、ウィジェット/UIの連打でも重複は発生しない。
     */
    suspend operator fun invoke(location: LatLng?): WorkSessionEntity = transactor.run {
        val today = DateUtils.today()
        val userName = settings.get().userName
        val now = System.currentTimeMillis()

        val existing = sessions.getByDate(today)
        if (existing != null && existing.status != SessionStatus.NOT_STARTED) return@run existing

        val base = existing ?: WorkSessionEntity(
            date = today,
            userName = userName,
            status = SessionStatus.NOT_STARTED,
        )
        val updated = base.copy(
            userName = userName.ifBlank { base.userName },
            status = SessionStatus.WORKING,
            startTime = now,
            startLatitude = location?.latitude,
            startLongitude = location?.longitude,
            startAddress = location?.address,
        )

        if (existing == null) {
            val id = sessions.insert(updated)
            updated.copy(id = id)
        } else {
            sessions.update(updated)
            updated
        }
    }
}
