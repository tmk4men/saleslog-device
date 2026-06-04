package com.example.saleslogdevice.domain.usecase

import android.net.Uri
import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.data.local.entity.AudioRecordingEntity
import com.example.saleslogdevice.data.repository.AudioRecordingRepository
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.service.AudioFileStore

sealed class ReceiveAudioResult {
    /** 指定の訪問に紐づけて保存した */
    data class LinkedToVisit(val entity: AudioRecordingEntity, val visitId: Long) : ReceiveAudioResult()
    /** 訪問を指定しなかったので未紐づけ録音として保存した */
    data class StoredUnlinked(val entity: AudioRecordingEntity) : ReceiveAudioResult()
    data object NoActiveSession : ReceiveAudioResult()
    data object ImportFailed : ReceiveAudioResult()
}

/**
 * レコーダーから共有された音声を端末内へ取り込み、（指定があれば）訪問に紐づける。
 * テキスト取り込み [ReceiveRecorderTextUseCase] と同じく visitLogId は画面側が決める。
 */
class ReceiveAudioUseCase(
    private val sessions: WorkSessionRepository,
    private val visits: VisitLogRepository,
    private val recordings: AudioRecordingRepository,
    private val store: AudioFileStore,
    private val transactor: Transactor,
) {
    suspend operator fun invoke(
        title: String?,
        uri: Uri,
        fileName: String,
        visitLogId: Long? = null,
    ): ReceiveAudioResult {
        // セッションが無いならコピー前に弾く（無駄なファイルを残さない）。
        if (sessions.getActiveWorking() == null) return ReceiveAudioResult.NoActiveSession
        val file = store.importToLocal(uri, fileName) ?: return ReceiveAudioResult.ImportFailed

        return transactor.run {
            val session = sessions.getActiveWorking() ?: return@run ReceiveAudioResult.NoActiveSession
            val targetVisit = visitLogId
                ?.let { visits.getById(it) }
                ?.takeIf { it.workSessionId == session.id }

            val base = AudioRecordingEntity(
                workSessionId = session.id,
                visitLogId = targetVisit?.id,
                title = (title ?: fileName).takeIf { it.isNotBlank() },
                filePath = file.absolutePath,
            )
            val id = recordings.insert(base)
            val saved = base.copy(id = id)
            if (targetVisit != null) ReceiveAudioResult.LinkedToVisit(saved, targetVisit.id)
            else ReceiveAudioResult.StoredUnlinked(saved)
        }
    }
}
