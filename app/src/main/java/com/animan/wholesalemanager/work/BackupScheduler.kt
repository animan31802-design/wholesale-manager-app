package com.animan.wholesalemanager.work

import android.content.Context
import androidx.work.*
import com.animan.wholesalemanager.utils.AppPreferences
import java.util.concurrent.TimeUnit

object BackupScheduler {

    private const val WORK_NAME = "wholesale_daily_backup"

    /**
     * Call this whenever:
     * 1. App starts (MainActivity.onCreate)
     * 2. User changes backup frequency in Settings
     * 3. User enables backup toggle
     */
    fun schedule(context: Context) {
        val enabled   = AppPreferences.isBackupEnabled(context)
        val frequency = AppPreferences.getBackupFrequency(context)

        if (!enabled || frequency == "Manual only") {
            // Cancel any scheduled backup
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }

        val intervalHours = when (frequency) {
            "Daily"  -> 24L
            "Weekly" -> 24L * 7
            else     -> return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<BackupWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            // Initial delay: start 1 hour after scheduling so it doesn't
            // run immediately on every app open
            .setInitialDelay(1, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        // KEEP_ALIVE: if a backup is already scheduled don't reschedule
        // Use UPDATE if frequency changed
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}