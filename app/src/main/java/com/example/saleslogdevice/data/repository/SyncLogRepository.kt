package com.example.saleslogdevice.data.repository

import com.example.saleslogdevice.data.local.dao.SyncLogDao
import com.example.saleslogdevice.data.local.entity.SyncLogEntity
import com.example.saleslogdevice.domain.model.SyncStatus

class SyncLogRepository(private val dao: SyncLogDao) {

    suspend fun getPending(): List<SyncLogEntity> = dao.getByStatus(SyncStatus.PENDING)

    suspend fun getFailed(): List<SyncLogEntity> = dao.getByStatus(SyncStatus.FAILED)

    suspend fun insert(entity: SyncLogEntity): Long = dao.insert(entity)

    suspend fun update(entity: SyncLogEntity) =
        dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
}
