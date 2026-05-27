package com.example.saleslogdevice.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val userName: String = "",
    val teamName: String = "",
    val driveFolderId: String = "",
    val spreadsheetId: String = "",
    val sheetName: String = "活動履歴",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
