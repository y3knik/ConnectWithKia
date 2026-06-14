package com.github.y3knik.connectwithkia.ui.credentials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.y3knik.connectwithkia.data.CredentialsRepository
import com.github.y3knik.connectwithkia.kia.KiaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CredentialsUiState {
    data class Editing(val email: String = "", val password: String = "", val pin: String = "") : CredentialsUiState

    data object Verifying : CredentialsUiState

    data object SavedSuccessfully : CredentialsUiState

    data class Error(val message: String) : CredentialsUiState
}

class CredentialsViewModel(
    private val repository: CredentialsRepository,
    private val kia: KiaClient,
) : ViewModel() {
    private var email: String = repository.read()?.email.orEmpty()
    private var password: String = repository.read()?.password.orEmpty()
    private var pin: String = repository.read()?.pin.orEmpty()

    private val _state = MutableStateFlow<CredentialsUiState>(CredentialsUiState.Editing(email, password, pin))
    val state: StateFlow<CredentialsUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        email = value
        emitEditing()
    }

    fun onPasswordChange(value: String) {
        password = value
        emitEditing()
    }

    fun onPinChange(value: String) {
        pin = value.filter { it.isDigit() }.take(4)
        emitEditing()
    }

    private fun emitEditing() {
        _state.value = CredentialsUiState.Editing(email, password, pin)
    }

    fun testAndSave() {
        if (email.isBlank() || password.isBlank() || pin.length != 4) {
            _state.value = CredentialsUiState.Error("Fill all three fields. PIN must be 4 digits.")
            return
        }
        // Snapshot the values being verified so that further edits during the network call
        // don't corrupt what we persist.
        val verifiedEmail = email
        val verifiedPassword = password
        val verifiedPin = pin
        _state.value = CredentialsUiState.Verifying
        viewModelScope.launch {
            kia.login(verifiedEmail, verifiedPassword)
                .onFailure {
                    _state.value = CredentialsUiState.Error(it.message ?: "Login failed")
                    return@launch
                }
            val vehicles = kia.vehicles().getOrNull()
            if (vehicles.isNullOrEmpty()) {
                _state.value = CredentialsUiState.Error("No vehicles found on the account")
                return@launch
            }
            repository.write(verifiedEmail, verifiedPassword, verifiedPin)
            repository.vehicleId = vehicles.first().id
            _state.value = CredentialsUiState.SavedSuccessfully
        }
    }
}
