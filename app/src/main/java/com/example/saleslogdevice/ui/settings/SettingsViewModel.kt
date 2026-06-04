package com.example.saleslogdevice.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
                it.copy(loaded = true, userName = s.userName, teamName = s.teamName)
            }
        }
    }

    fun onUserName(value: String) = _state.update { it.copy(userName = value) }
    fun onTeam(value: String) = _state.update { it.copy(teamName = value) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun save() = viewModelScope.launch {
        val s = state.value
        // 既存設定を保持しつつ氏名・チーム名だけ更新（未使用の旧Google項目は既定値のまま）。
        val current = locator.userSettings.get()
        locator.userSettings.save(
            current.copy(
                userName = s.userName.trim(),
                teamName = s.teamName.trim(),
            )
        )
        _state.update { it.copy(message = "設定を保存しました") }
    }
}
