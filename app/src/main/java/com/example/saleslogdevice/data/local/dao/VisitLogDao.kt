package com.example.saleslogdevice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.saleslogdevice.data.local.entity.VisitLogEntity
import com.example.saleslogdevice.domain.model.VisitStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitLogDao {

    @Query("SELECT * FROM visit_logs WHERE workSessionId = :sessionId ORDER BY checkInTime ASC")
    fun observeBySession(sessionId: Long): Flow<List<VisitLogEntity>>

    @Query("SELECT * FROM visit_logs WHERE workSessionId = :sessionId ORDER BY checkInTime ASC")
    suspend fun getBySession(sessionId: Long): List<VisitLogEntity>

    @Query("SELECT * FROM visit_logs WHERE workSessionId = :sessionId AND status = :status ORDER BY id DESC LIMIT 1")
    suspend fun getActive(sessionId: Long, status: VisitStatus = VisitStatus.CHECKED_IN): VisitLogEntity?

    @Query("SELECT * FROM visit_logs WHERE id = :id")
    suspend fun getById(id: Long): VisitLogEntity?

    @Query("SELECT COUNT(*) FROM visit_logs WHERE workSessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Insert
    suspend fun insert(entity: VisitLogEntity): Long

    @Update
    suspend fun update(entity: VisitLogEntity)
}
