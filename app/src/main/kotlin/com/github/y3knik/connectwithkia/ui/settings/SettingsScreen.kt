package com.github.y3knik.connectwithkia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.y3knik.connectwithkia.di.AppContainer

@Composable
fun SettingsScreen() {
    val container = AppContainer.get(LocalContext.current)
    val vm: SettingsViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer { SettingsViewModel(container.settings, container.credentials) }
                },
        )
    val s by vm.state.collectAsState()
    var sliderValue by remember(s.lockDelayMinutes) {
        mutableFloatStateOf(s.lockDelayMinutes.toFloat())
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Status-bar countdown")
            Switch(checked = s.highProminence, onCheckedChange = vm::setHighProminence)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Success notification")
            Switch(checked = s.successNotification, onCheckedChange = vm::setSuccessNotification)
        }
        Text("Lock delay: ${sliderValue.toInt()} min")
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { vm.setDelay(sliderValue.toInt()) },
            valueRange = 1f..15f,
            steps = 13,
        )
        OutlinedButton(onClick = vm::signOut) { Text("Sign out") }
    }
}
