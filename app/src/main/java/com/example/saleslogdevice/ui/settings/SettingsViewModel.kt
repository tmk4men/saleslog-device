package com.example.saleslogdevice.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.data.local.entity.UserSettingsEntity
import com.example.saleslogdevice.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loaded: Boolean = false,
    val userName: String = "",
    val teamName: String = "",
    val driveFolderId: String = "",
    val spreadsheetId: String = "",
    val sheetName: String = "活動履歴",
    val message: String? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = locator.userSettings.get()
            _state.update {
                it.copy(
                    loaded = true,
                    userName = s.userName,
                    teamName = s.teamName,
                    driveFolderId = s.driveFolderId,
                    spreadsheetId = s.spreadsheetId,
                    sheetName = s.sheetName.ifBlank { "活動履歴" },
                )
            }
        }
    }

    fun onUserName(value: String) = _state.update { it.copy(userName = value) }
    fun onTeam(value: String) = _state.update { it.copy(teamName = value) }
    fun onDriveFolder(value: String) = _state.update { it.copy(driveFolderId = value) }
    fun onSpreadsheet(value: String) = _state.update { it.copy(spreadsheetId = value) }
    fun onSheetName(value: String) = _state.update { it.copy(sheetName = value) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun save() = viewModelScope.launch {
        val s = state.value
        locator.userSettings.save(
            UserSettingsEntity(
                userName = s.userName.trim(),
                teamName = s.teamName.trim(),
                driveFolderId = s.driveFolderId.trim(),
                spreadsheetId = s.spreadsheetId.trim(),
                sheetName = s.sheetName.trim().ifBlank { "活動履歴" },
            )
        )
        _state.update { it.copy(message = "設定を保存しました") }
    }

    fun testDrive() = viewModelScope.launch {
        val s = state.value
        val result = locator.driveRepo.saveReport(
            folderId = s.driveFolderId,
            fileName = "test_${System.currentTimeMillis()}.md",
            markdown = "# テスト保存\nこれはMock Driveのテストです。",
        )
        _state.update {
            it.copy(message = result.fold(
                onSuccess = { url -> "Driveテスト保存OK: $url" },
                onFailure = { ex -> "Driveテスト保存失敗: ${ex.message}" },
            ))
        }
    }

    fun testSheets() = viewModelScope.launch {
        val s = state.value
        val result = locator.sheetsRepo.appendRows(
            spreadsheetId = s.spreadsheetId,
            sheetName = s.sheetName,
            header = emptyList(),
            rows = listOf(listOf("テスト", s.userName, "OK")),
        )
        _state.update {
            it.copy(message = result.fold(
                onSuccess = { "CSVテスト記録OK" },
                onFailure = { ex -> "CSVテスト記録失敗: ${ex.message}" },
            ))
        }
    }
}
