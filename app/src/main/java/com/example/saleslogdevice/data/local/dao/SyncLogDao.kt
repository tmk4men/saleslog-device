package com.example.saleslogdevice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.saleslogdevice.data.local.entity.SyncLogEntity
import com.example.saleslogdevice.domain.model.SyncStatus

@Dao
interface SyncLogDao {

    @Query("SELECT * FROM sync_logs WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: SyncStatus): List<SyncLogEntity>

    @Insert
    suspend fun insert(entity: SyncLogEntity): Long

    @Update
    suspend fun update(entity: SyncLogEntity)
}
