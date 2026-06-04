package com.example.saleslogdevice.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * レコーダーから共有された音声ファイルを端末内にコピーして紐づけ管理する。
 * 実体は <filesDir>/recordings/ にコピー済みで、[filePath] はその絶対パス。
 */
@Entity(
    tableName = "audio_recordings",
    foreignKeys = [
        ForeignKey(
            entity = WorkSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workSessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("workSessionId"), Index("visitLogId")],
)
data class AudioRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val workSessionId: Long,
    val visitLogId: Long? = null,
    val title: String? = null,
    /** アプリ内ストレージにコピーした音声ファイルの絶対パス。 */
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis(),
)
