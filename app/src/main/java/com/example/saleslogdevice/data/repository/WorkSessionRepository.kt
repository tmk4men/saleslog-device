package com.example.saleslogdevice.data.repository

import com.example.saleslogdevice.data.local.dao.WorkSessionDao
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.domain.model.SessionStatus
import com.example.saleslogdevice.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow

class WorkSessionRepository(private val dao: WorkSessionDao) {

    fun observeByDate(date: String): Flow<WorkSessionEntity?> = dao.observeByDate(date)

    suspend fun getByDate(date: String): WorkSessionEntity? = dao.getByDate(date)

    suspend fun getById(id: Long): WorkSessionEntity? = dao.getById(id)

    /** 「今日のWORKINGセッション」のみを返す。前日以前のWORKINGはここに引っかからない。 */
    suspend fun getActiveWorking(): WorkSessionEntity? =
        dao.getByDateAndStatus(DateUtils.today(), SessionStatus.WORKING)

    suspend fun insert(entity: WorkSessionEntity): Long = dao.insert(entity)

    suspend fun update(entity: WorkSessionEntity) =
        dao.update(entity.copy(updatedAt = System.currentTimeMillis()))
}
