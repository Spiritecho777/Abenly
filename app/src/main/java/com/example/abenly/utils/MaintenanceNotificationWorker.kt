package com.example.abenly.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.abenly.R
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class MaintenanceNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()

        // 1. Liste des éléments de base (par défaut)
        val defaultKeys = listOf(
            Triple("vidange", R.string.maintenance_engine_oil, Pair(12L, 7L)),
            Triple("controle_technique", R.string.maintenance_technical_control, Pair(24L, 14L)),
            Triple("pneus", R.string.maintenance_tires, Pair(1L, 7L)),
            Triple("freins", R.string.maintenance_brakes, Pair(24L, 7L)),
            Triple("essuie_glaces", R.string.maintenance_wipers, Pair(12L, 7L)),
            Triple("batterie", R.string.maintenance_battery, Pair(48L, 7L))
        )

        for ((key, titleRes, config) in defaultKeys) {
            val (maxMonths, warningDays) = config
            val title = context.getString(titleRes)
            checkAndSendNotification(key, title, maxMonths, warningDays, today)
        }

        // 2. Traitement des éléments personnalisés enregistrés dans DataStore
        val customItemsSet = MaintenancePreferences.getCustomItems(context).firstOrNull() ?: emptySet()
        for (itemString in customItemsSet) {
            val parts = itemString.split("|")
            if (parts.size == 2) {
                val key = parts[0]
                val maxMonths = parts[1].toLongOrNull() ?: 12L
                // Warning fixé par défaut à 7 jours avant l'échéance pour les champs perso
                checkAndSendNotification(key, key, maxMonths, 7L, today)
            }
        }

        return Result.success()
    }

    private suspend fun checkAndSendNotification(
        key: String,
        title: String,
        maxMonths: Long,
        warningDays: Long,
        today: LocalDate
    ) {
        val savedTimestamp = MaintenancePreferences.getLastDate(context, key).firstOrNull() ?: return

        val lastDate = Instant.ofEpochMilli(savedTimestamp)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()

        val dueDate = lastDate.plusMonths(maxMonths)
        val daysRemaining = ChronoUnit.DAYS.between(today, dueDate)

        // Envoie si dans la fenêtre de pré-échéance ou si la date est dépassée
        if (daysRemaining <= warningDays) {
            val message = if (daysRemaining < 0L) {
                "Échéance dépassée ! Pensez à faire votre entretien."
            } else {
                "Échéance dans $daysRemaining jour(s)."
            }
            sendNotification(
                id = key.hashCode(),
                title = "Rappel Entretien — $title",
                message = message
            )
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val channelId = "maintenance_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rappels d'entretien",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            id,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        manager.notify(id, builder.build())
    }
}