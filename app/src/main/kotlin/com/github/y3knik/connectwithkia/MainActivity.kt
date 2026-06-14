package com.github.y3knik.connectwithkia

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.github.y3knik.connectwithkia.ui.AppNavigation
import com.github.y3knik.connectwithkia.ui.theme.ConnectWithKiaTheme

class MainActivity : ComponentActivity() {
    private val notifPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { /* result ignored; UI just rechecks on next resume */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        ensureExactAlarm()
        ensureBatteryOptExempt()
        setContent {
            ConnectWithKiaTheme {
                AppNavigation()
            }
        }
    }

    private fun ensureExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (am.canScheduleExactAlarms()) return
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }

    private fun ensureBatteryOptExempt() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName")),
        )
    }
}
