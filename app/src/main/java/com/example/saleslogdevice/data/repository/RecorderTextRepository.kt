package com.example.saleslogdevice.data.repository

import com.example.saleslogdevice.data.local.dao.RecorderTextDao
import com.example.saleslogdevice.data.local.entity.RecorderTextEntity
import kotlinx.coroutines.flow.Flow

class RecorderTextRepository(private val dao: RecorderTextDao) {

    fun observeBySession(sessionId: Long): Flow<List<RecorderTextEntity>> =
        dao.observeBySession(sessionId)

    suspend fun getBySession(sessionId: Long): List<RecorderTextEntity> =
        dao.getBySession(sessionId)

    suspend fun getByVisit(visitId: Long): List<RecorderTextEntity> =
        dao.getByVisit(visitId)

    suspend fun countBySession(sessionId: Long): Int = dao.countBySession(sessionId)

    suspend fun insert(entity: RecorderTextEntity): Long = dao.insert(entity)

    suspend fun update(entity: RecorderTextEntity) =
        dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
}
