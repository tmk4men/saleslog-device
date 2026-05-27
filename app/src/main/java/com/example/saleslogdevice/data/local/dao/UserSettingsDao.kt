package com.example.saleslogdevice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.saleslogdevice.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    fun observe(id: Int = UserSettingsEntity.SINGLETON_ID): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = UserSettingsEntity.SINGLETON_ID): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserSettingsEntity)
}
