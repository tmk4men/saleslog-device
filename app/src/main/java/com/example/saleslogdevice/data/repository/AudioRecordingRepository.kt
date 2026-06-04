package com.example.saleslogdevice.data.repository

import com.example.saleslogdevice.data.local.dao.AudioRecordingDao
import com.example.saleslogdevice.data.local.entity.AudioRecordingEntity

class AudioRecordingRepository(private val dao: AudioRecordingDao) {

    suspend fun getBySession(sessionId: Long): List<AudioRecordingEntity> =
        dao.getBySession(sessionId)

    suspend fun getByVisit(visitId: Long): List<AudioRecordingEntity> =
        dao.getByVisit(visitId)

    suspend fun insert(entity: AudioRecordingEntity): Long = dao.insert(entity)
}
