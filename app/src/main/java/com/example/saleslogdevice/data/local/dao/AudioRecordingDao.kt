package com.example.saleslogdevice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.saleslogdevice.data.local.entity.AudioRecordingEntity

@Dao
interface AudioRecordingDao {

    @Query("SELECT * FROM audio_recordings WHERE workSessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getBySession(sessionId: Long): List<AudioRecordingEntity>

    @Query("SELECT * FROM audio_recordings WHERE visitLogId = :visitId ORDER BY createdAt ASC")
    suspend fun getByVisit(visitId: Long): List<AudioRecordingEntity>

    @Insert
    suspend fun insert(entity: AudioRecordingEntity): Long
}
