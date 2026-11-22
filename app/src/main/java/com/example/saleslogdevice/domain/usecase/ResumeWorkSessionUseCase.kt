package com.example.saleslogdevice.domain.usecase

import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.util.DateUtils

/**
 * 営業終了（日報作成）後に「もう一度営業に戻る」ためのユースケース。
 * 今日の FINISHED セッションを WORKING に戻し、終了時刻をクリアして再開する。
 * 同じセッションを再開するので、これまでの訪問・録音はそのまま残り、追加で訪問できる。
 */
class ResumeWorkSessionUseCase(
    private val sessions: WorkSessionRepository,
    private val transactor: Transactor,
) {
    /**
     * 戻り値は再開後のセッション。今日のセッションが無ければ null、
     * すでに稼働中（WORKING / NOT_STARTED）ならそのまま返す（再開不要）。
     */
    suspend operator fun invoke(): WorkSessionEntity? = transactor.run {
        val today = sessions.getByDate(DateUtils.today()) ?: return@run null
        if (today.status != SessionStatus.FINISHED) return@run today
        val resumed = today.copy(
            status = SessionStatus.WORKING,
            endTime = null,
            endLatitude = null,
            endLongitude = null,
            endAddress = null,
        )
        sessions.update(resumed)
        resumed
    }
}
