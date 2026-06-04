package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.domain.model.LatLng

/**
 * 営業終了の全工程をまとめる集約UseCase。
 *
 *   endWorkSession → generateDailyReport → appendActivityToSheets(端末内CSV)
 *
 * Home画面とWidget両方からこれを呼ぶことで、入口によって挙動が違うという矛盾をなくす。
 * 日報はDBから常に再生成でき、活動履歴は端末内CSVに追記される。
 */
class FinalizeDayUseCase(
    private val endWorkSession: EndWorkSessionUseCase,
    private val generateDailyReport: GenerateDailyReportUseCase,
    private val appendActivityToSheets: AppendActivityToSheetsUseCase,
) {
    sealed class Result {
        data class Success(
            val session: WorkSessionEntity,
            val csvSuccess: Boolean,
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
                    Result.Success(ended.session, csvSuccess = false)
                } else {
                    val csv = appendActivityToSheets(ended.session.id)
                    Result.Success(session = ended.session, csvSuccess = csv.isSuccess)
                }
            }
        }
}
