package com.github.y3knik.connectwithkia.ui.settings

import androidx.lifecycle.ViewModel
import com.github.y3knik.connectwithkia.data.AppSettings
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val highProminence: Boolean,
    val successNotification: Boolean,
    val lockDelayMinutes: Int,
)

class SettingsViewModel(
    private val settings: AppSettings,
    private val credentials: CredentialsRepository,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            SettingsUiState(
                highProminence = settings.highProminenceCountdown,
                successNotification = settings.successNotification,
                lockDelayMinutes = settings.lockDelayMinutes,
            ),
        )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setHighProminence(value: Boolean) {
        settings.highProminenceCountdown = value
        _state.value = _state.value.copy(highProminence = value)
    }

    fun setSuccessNotification(value: Boolean) {
        settings.successNotification = value
        _state.value = _state.value.copy(successNotification = value)
    }

    fun setDelay(value: Int) {
        settings.lockDelayMinutes = value
        _state.value = _state.value.copy(lockDelayMinutes = settings.lockDelayMinutes)
    }

    fun signOut() {
        credentials.clear()
    }
}
