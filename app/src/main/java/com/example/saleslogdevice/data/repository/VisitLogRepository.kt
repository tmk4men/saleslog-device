package com.example.saleslogdevice.data.repository

import com.example.saleslogdevice.data.local.dao.VisitLogDao
import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import kotlinx.coroutines.flow.Flow

class VisitLogRepository(private val dao: VisitLogDao) {

    fun observeBySession(sessionId: Long): Flow<List<VisitLogEntity>> =
        dao.observeBySession(sessionId)

    suspend fun getBySession(sessionId: Long): List<VisitLogEntity> =
        dao.getBySession(sessionId)

    suspend fun getActive(sessionId: Long): VisitLogEntity? = dao.getActive(sessionId)

    suspend fun countBySession(sessionId: Long): Int = dao.countBySession(sessionId)

    suspend fun getById(id: Long): VisitLogEntity? = dao.getById(id)

    suspend fun insert(entity: VisitLogEntity): Long = dao.insert(entity)

    suspend fun update(entity: VisitLogEntity) =
        dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
}
