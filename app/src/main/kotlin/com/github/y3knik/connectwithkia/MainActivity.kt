package com.github.y3knik.connectwithkia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.github.y3knik.connectwithkia.ui.AppNavigation
import com.github.y3knik.connectwithkia.ui.theme.ConnectWithKiaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConnectWithKiaTheme {
                AppNavigation()
            }
        }
    }
}
