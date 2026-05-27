package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.domain.model.LatLng

/**
 * 営業終了の全工程をまとめる集約UseCase。
 *
 *   endWorkSession → generateDailyReport → syncReportToDrive → appendActivityToSheets
 *
 * Home画面とWidget両方からこれを呼ぶことで、入口によって挙動が違うという矛盾をなくす。
 * Drive/SheetsはMVPではMockなので基本成功するが、失敗時もローカルに日報は残る。
 */
class FinalizeDayUseCase(
    private val endWorkSession: EndWorkSessionUseCase,
    private val generateDailyReport: GenerateDailyReportUseCase,
    private val syncReportToDrive: SyncReportToDriveUseCase,
    private val appendActivityToSheets: AppendActivityToSheetsUseCase,
) {
    sealed class Result {
        data class Success(
            val session: WorkSessionEntity,
            val driveUrl: String?,
            val driveSuccess: Boolean,
            val sheetsSuccess: Boolean,
        ) : Result()
        data object NoActiveSession : Result()
        data object VisitOngoing : Result()
    }

    suspend operator fun invoke(location: LatLng?): Result =
        when (val ended = endWorkSession(location)) {
            EndWorkResult.NoActiveSession -> Result.NoActiveSession
            EndWorkResult.VisitOngoing -> Result.VisitOngoing
            is EndWorkResult.Success -> {
                val report = generateDailyReport(ended.session.id)
                if (report == null) {
                    Result.Success(ended.session, driveUrl = null, driveSuccess = false, sheetsSuccess = false)
                } else {
                    val drive = syncReportToDrive(ended.session.id, report.markdown)
                    val sheets = appendActivityToSheets(ended.session.id, drive.driveUrl)
                    Result.Success(
                        session = ended.session,
                        driveUrl = drive.driveUrl,
                        driveSuccess = drive.isSuccess,
                        sheetsSuccess = sheets.isSuccess,
                    )
                }
            }
        }
}
