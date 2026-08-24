package com.example.abenly.utils

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    fun scheduleDailyCheck(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Calcul du délai jusqu'à 09h00 du matin
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(currentDate)) {
                add(Calendar.HOUR_OF_DAY, 24)
            }
        }
        val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

        // Planification périodique toutes les 24h
        val dailyWorkRequest = PeriodicWorkRequestBuilder<MaintenanceNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "MaintenanceDailyCheck",
            ExistingPeriodicWorkPolicy.KEEP, // Conserve le planning même si l'app réouvre
            dailyWorkRequest
        )
    }
}