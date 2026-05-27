package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.SyncLogEntity
import com.example.saleslogdevice.data.repository.GoogleDriveRepository
import com.example.saleslogdevice.data.repository.SyncLogRepository
import com.example.saleslogdevice.data.repository.UserSettingsRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.SyncStatus
import com.example.saleslogdevice.domain.model.SyncTarget

data class DriveSyncResult(val driveUrl: String?, val isSuccess: Boolean, val errorMessage: String? = null)

class SyncReportToDriveUseCase(
    private val drive: GoogleDriveRepository,
    private val sessions: WorkSessionRepository,
    private val settings: UserSettingsRepository,
    private val syncLogs: SyncLogRepository,
) {
    suspend operator fun invoke(sessionId: Long, markdown: String): DriveSyncResult {
        val session = sessions.getById(sessionId)
            ?: return DriveSyncResult(null, false, "session not found")
        val s = settings.get()
        val fileName = "${session.date}_${session.userName.ifBlank { "anonymous" }}_営業日報.md"

        val result = drive.saveReport(s.driveFolderId, fileName, markdown)
        return result.fold(
            onSuccess = { url ->
                sessions.update(session.copy(syncedToDrive = true))
                syncLogs.insert(
                    SyncLogEntity(
                        targetType = SyncTarget.DRIVE,
                        targetId = session.id,
                        status = SyncStatus.SUCCESS,
                    )
                )
                DriveSyncResult(url, true)
            },
            onFailure = { ex ->
                syncLogs.insert(
                    SyncLogEntity(
                        targetType = SyncTarget.DRIVE,
                        targetId = session.id,
                        status = SyncStatus.FAILED,
                        errorMessage = ex.message,
                    )
                )
                DriveSyncResult(null, false, ex.message)
            }
        )
    }
}
