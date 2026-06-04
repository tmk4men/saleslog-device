package com.example.saleslogdevice.di

import android.content.Context
import com.example.saleslogdevice.data.ai.OnDeviceSummarizer
import com.example.saleslogdevice.data.local.AppDatabase
import com.example.saleslogdevice.data.local.Transactor
import com.example.saleslogdevice.domain.ai.Summarizer
import com.example.saleslogdevice.data.repository.GoogleSheetsRepository
import com.example.saleslogdevice.data.repository.GoogleSheetsRepositoryMock
import com.example.saleslogdevice.data.repository.AudioRecordingRepository
import com.example.saleslogdevice.data.repository.RecorderTextRepository
import com.example.saleslogdevice.data.repository.SyncLogRepository
import com.example.saleslogdevice.data.repository.UserSettingsRepository
import com.example.saleslogdevice.data.repository.VisitLogRepository
import com.example.saleslogdevice.data.repository.WorkSessionRepository
import com.example.saleslogdevice.domain.usecase.AppendActivityToSheetsUseCase
import com.example.saleslogdevice.domain.usecase.CheckInUseCase
import com.example.saleslogdevice.domain.usecase.CheckOutUseCase
import com.example.saleslogdevice.domain.usecase.EndWorkSessionUseCase
import com.example.saleslogdevice.domain.usecase.FinalizeDayUseCase
import com.example.saleslogdevice.domain.usecase.GenerateDailyReportUseCase
import com.example.saleslogdevice.domain.usecase.ReceiveAudioUseCase
import com.example.saleslogdevice.domain.usecase.ReceiveRecorderTextUseCase
import com.example.saleslogdevice.domain.usecase.StartWorkSessionUseCase
import com.example.saleslogdevice.service.AudioFileStore
import com.example.saleslogdevice.service.LocationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 軽量DI。MVP段階ではDIライブラリを入れずにこれで賄う。
 * Drive/Sheets は Mock を差し込んでいる。本番API実装が出来たら driveRepo / sheetsRepo を差し替える。
 */
class ServiceLocator(appContext: Context) {

    private val db = AppDatabase.get(appContext)
    val transactor = Transactor(db)

    /** Activity/ViewModelに紐づかないバックグラウンド処理（Widget受信など）の共通スコープ。 */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userSettings = UserSettingsRepository(db.userSettingsDao())
    val workSessions = WorkSessionRepository(db.workSessionDao())
    val visitLogs = VisitLogRepository(db.visitLogDao())
    val recorderTexts = RecorderTextRepository(db.recorderTextDao())
    val syncLogs = SyncLogRepository(db.syncLogDao())
    val audioRecordings = AudioRecordingRepository(db.audioRecordingDao())

    val audioFileStore = AudioFileStore(appContext)

    private val sheetsRepoImpl = GoogleSheetsRepositoryMock(appContext)
    val sheetsRepo: GoogleSheetsRepository = sheetsRepoImpl

    /** 蓄積済み活動CSVファイル。まだ1件も記録が無ければ null。共有/書き出し用。 */
    fun activityCsvFile(): java.io.File? =
        sheetsRepoImpl.fileFor("", AppendActivityToSheetsUseCase.CSV_NAME).takeIf { it.exists() }

    /**
     * 端末内要約。Gemini Nano 対応端末なら primary を差し込む（現状は未配線でヒューリスティックにフォールバック）。
     * 対応後は OnDeviceSummarizer(primary = GeminiNanoSummarizer()) へ変更するだけでよい。
     */
    val summarizer: Summarizer = OnDeviceSummarizer()

    val locationProvider = LocationProvider(appContext)

    val startWorkSession = StartWorkSessionUseCase(workSessions, userSettings, transactor)
    val endWorkSession = EndWorkSessionUseCase(workSessions, visitLogs, transactor)
    val checkIn = CheckInUseCase(workSessions, visitLogs, transactor)
    val checkOut = CheckOutUseCase(workSessions, visitLogs)
    val receiveRecorderText = ReceiveRecorderTextUseCase(workSessions, visitLogs, recorderTexts, transactor, summarizer)
    val receiveAudio = ReceiveAudioUseCase(workSessions, visitLogs, audioRecordings, audioFileStore, transactor)
    val generateDailyReport = GenerateDailyReportUseCase(workSessions, visitLogs, recorderTexts)
    val appendActivityToSheets = AppendActivityToSheetsUseCase(
        sheetsRepo, workSessions, visitLogs, recorderTexts, syncLogs,
    )
    val finalizeDay = FinalizeDayUseCase(endWorkSession, generateDailyReport, appendActivityToSheets)

    companion object {
        @Volatile private var instance: ServiceLocator? = null

        fun get(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context.applicationContext).also { instance = it }
            }
    }
}
