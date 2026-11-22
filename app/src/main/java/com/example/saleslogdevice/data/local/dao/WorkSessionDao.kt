package com.example.saleslogdevice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.saleslogdevice.data.local.entity.WorkSessionEntity
import com.example.saleslogdevice.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkSessionDao {

    @Query("SELECT * FROM work_sessions WHERE date = :date ORDER BY id DESC LIMIT 1")
    fun observeByDate(date: String): Flow<WorkSessionEntity?>

    @Query("SELECT * FROM work_sessions WHERE date = :date ORDER BY id DESC LIMIT 1")
    suspend fun getByDate(date: String): WorkSessionEntity?

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkSessionEntity?

    /** 実際に稼働した日（開始済み）の履歴を新しい順に返す。未開始のプレースホルダは除外する。 */
    @Query("SELECT * FROM work_sessions WHERE startTime IS NOT NULL ORDER BY date DESC, id DESC")
    suspend fun getRecent(): List<WorkSessionEntity>

    @Query("SELECT * FROM work_sessions WHERE status = :status ORDER BY id DESC LIMIT 1")
    suspend fun getLatestByStatus(status: SessionStatus): WorkSessionEntity?

    @Query("SELECT * FROM work_sessions WHERE date = :date AND status = :status ORDER BY id DESC LIMIT 1")
    suspend fun getByDateAndStatus(date: String, status: SessionStatus): WorkSessionEntity?

    @Insert
    suspend fun insert(entity: WorkSessionEntity): Long

    @Update
    suspend fun update(entity: WorkSessionEntity)
}
