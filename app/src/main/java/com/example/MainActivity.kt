package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.SessionRepository
import com.example.ui.FlowTimerApp
import com.example.ui.FlowTimerViewModel
import com.example.ui.FlowTimerViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FlowHapticManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pre-warm / initialize low-latency haptics service instantly
        FlowHapticManager.initialize(this)

        // Initialize local database database and repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SessionRepository(database.sessionDao())

        // Initialize Viewmodel with Factory
        val viewModel = ViewModelProvider(
            this,
            FlowTimerViewModelFactory(repository)
        )[FlowTimerViewModel::class.java]

        setContent {
            MyApplicationTheme {
                FlowTimerApp(viewModel = viewModel)
            }
        }
    }
}
