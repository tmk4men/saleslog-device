package com.example.saleslogdevice.ui.checkin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.saleslogdevice.di.ServiceLocator
import com.example.saleslogdevice.domain.usecase.CheckInResult
import com.example.saleslogdevice.widget.SalesLogWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckInUiState(
    val clientName: String = "",
    val memo: String = "",
    val submitting: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

class CheckInViewModel(application: Application) : AndroidViewModel(application) {

    private val locator = ServiceLocator.get(application)

    private val _state = MutableStateFlow(CheckInUiState())
    val state: StateFlow<CheckInUiState> = _state.asStateFlow()

    fun onClientNameChange(value: String) = _state.update { it.copy(clientName = value) }
    fun onMemoChange(value: String) = _state.update { it.copy(memo = value) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun submit() = viewModelScope.launch {
        _state.update { it.copy(submitting = true) }
        val location = locator.locationProvider.getCurrent()
        val result = locator.checkIn(state.value.clientName, state.value.memo, location)
        val msg = when (result) {
            is CheckInResult.Success -> {
                _state.update { it.copy(submitting = false, done = true, message = "チェックインしました") }
                SalesLogWidgetProvider.requestUpdate(getApplication())
                return@launch
            }
            CheckInResult.NoActiveSession -> "営業開始後にチェックインできます。"
            CheckInResult.AlreadyVisiting -> "他の訪問中です。先にチェックアウトしてください。"
        }
        _state.update { it.copy(submitting = false, message = msg) }
    }
}
