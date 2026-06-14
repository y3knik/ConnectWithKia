package com.github.y3knik.connectwithkia.ui.credentials

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.y3knik.connectwithkia.di.AppContainer

@Composable
fun CredentialsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = AppContainer.get(context)
    val vm: CredentialsViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { CredentialsViewModel(container.credentials, container.kiaClient) }
                },
        )
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val editing = (state as? CredentialsUiState.Editing) ?: CredentialsUiState.Editing()
        OutlinedTextField(
            value = editing.email,
            onValueChange = vm::onEmailChange,
            label = { Text("Kia Connect email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = editing.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = editing.pin,
            onValueChange = vm::onPinChange,
            label = { Text("4-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        Button(onClick = { vm.testAndSave() }, enabled = state !is CredentialsUiState.Verifying) {
            Text("Test and save")
        }
        when (val s = state) {
            is CredentialsUiState.Verifying -> CircularProgressIndicator()
            is CredentialsUiState.SavedSuccessfully -> Text("Saved.")
            is CredentialsUiState.Error -> Text(s.message)
            else -> Unit
        }
    }
}
