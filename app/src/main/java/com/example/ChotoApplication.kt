package com.example

import android.app.Application
import com.example.compression.VideoInspector
import com.example.data.ChotoDatabase
import com.example.data.HistoryRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ChotoApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { ChotoDatabase.getInstance(this) }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            VideoInspector.cleanupOldTempFiles(this@ChotoApplication)
        }
    }
}
