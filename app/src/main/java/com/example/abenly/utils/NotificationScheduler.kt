package com.example.abenly.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    fun scheduleDailyCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<MaintenanceNotificationWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MaintenanceDailyCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}