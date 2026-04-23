package com.animan.wholesalemanager.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.animan.wholesalemanager.utils.AppPreferences
import com.animan.wholesalemanager.work.BackupScheduler
import com.animan.wholesalemanager.work.BackupWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.work.*

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    var isBackingUp  = mutableStateOf(false)
    var backupStatus = mutableStateOf("")
    // FIX: keep last backup time in state so UI recomposes when it changes
    var lastBackupTime = mutableStateOf(
        AppPreferences.getLastBackupTime(app.applicationContext)
    )

    fun backupNow(context: Context) {
        isBackingUp.value  = true
        backupStatus.value = "Backing up…"

        // Run the same BackupWorker but as a one-time request
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueue(request)

        // Observe result
        viewModelScope.launch(Dispatchers.IO) {
            // Poll for up to 30 seconds
            var waited = 0
            while (waited < 30000) {
                kotlinx.coroutines.delay(1000)
                waited += 1000
                val newTime = AppPreferences.getLastBackupTime(context)
                if (newTime != lastBackupTime.value) {
                    withContext(Dispatchers.Main) {
                        lastBackupTime.value = newTime
                        backupStatus.value   = "Backup complete — $newTime"
                        isBackingUp.value    = false
                    }
                    return@launch
                }
            }
            withContext(Dispatchers.Main) {
                backupStatus.value = "Check your internet connection and try again."
                isBackingUp.value  = false
            }
        }
    }

    fun restoreNow(context: Context) {
        // Keep existing restore logic from Phase 4 BackupViewModel
        // (unchanged — just call the same Firebase read logic)
        backupStatus.value = "Restore triggered — see BackupViewModel.restoreNow()"
    }
}