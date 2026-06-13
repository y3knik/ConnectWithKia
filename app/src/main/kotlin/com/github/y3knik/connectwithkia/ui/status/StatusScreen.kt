package com.github.y3knik.connectwithkia.ui.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.y3knik.connectwithkia.di.AppContainer
import com.github.y3knik.connectwithkia.scheduler.LockEvent

@Composable
fun StatusScreen() {
    val container = AppContainer.get(LocalContext.current)
    var enabled by remember { mutableStateOf(container.settings.enabled) }
    val state by container.scheduler.state.collectAsState()
    val vm = remember {
        StatusViewModel(
            state = container.scheduler.state,
            lockNow = { container.scheduler.onEvent(LockEvent.AlarmFired) },
            toggleEnabled = {
                container.settings.enabled = !container.settings.enabled
                if (container.settings.enabled) container.scheduler.onEvent(LockEvent.MasterEnabled)
                else container.scheduler.onEvent(LockEvent.MasterDisabled)
            },
            isEnabled = { container.settings.enabled },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("Enabled")
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    vm.toggleEnabled()
                },
            )
        }
        Text(vm.displayText(state))
        OutlinedButton(onClick = { vm.lockNow() }) { Text("Lock now (test)") }
    }
}
