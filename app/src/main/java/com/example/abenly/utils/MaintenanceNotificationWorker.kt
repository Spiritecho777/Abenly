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
        val keys = listOf(
            Triple("vidange", R.string.maintenance_engine_oil, Pair(12L, 7L)),
            Triple("controle_technique", R.string.maintenance_technical_control, Pair(24L, 14L)),
            Triple("pneus", R.string.maintenance_tires, Pair(1L, 7L)),
            Triple("freins", R.string.maintenance_brakes, Pair(24L, 7L)),
            Triple("essuie_glaces", R.string.maintenance_wipers, Pair(12L, 7L)),
            Triple("batterie", R.string.maintenance_battery, Pair(48L, 7L))
        )

        val today = LocalDate.now()

        for ((key, titleRes, config) in keys) {
            val (maxMonths, warningDays) = config

            val savedTimestamp = MaintenancePreferences.getLastDate(context, key).firstOrNull()
            if (savedTimestamp != null) {
                val lastDate = Instant.ofEpochMilli(savedTimestamp)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()

                val dueDate = lastDate.plusMonths(maxMonths)
                val daysRemaining = ChronoUnit.DAYS.between(today, dueDate)

                // Envoie la notif si on est dans la fenêtre (<= 7 ou 14 jours) OU si c'est dépassé (< 0)
                if (daysRemaining <= warningDays) {
                    val title = context.getString(titleRes)
                    sendNotification(
                        id = key.hashCode(),
                        title = "Rappel Entretien — $title",
                        message = if (daysRemaining < 0L) {
                            "Échéance dépassée ! Pensez à faire votre entretien."
                        } else {
                            "Échéance dans $daysRemaining jour(s)."
                        }
                    )
                }
            }
        }
        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        val channelId = "maintenance_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Le canal est obligatoire depuis Android 8 (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rappels d'entretien",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Intent pour rouvrir l'app au clic sur la notif
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
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Active le son et le vibreur par défaut sur Android 7
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        manager.notify(id, builder.build())
    }
}