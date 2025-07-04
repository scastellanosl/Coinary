package com.example.coinary.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.coinary.receivers.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.coinary.data.ReminderItem

object NotificationScheduler {

    const val CHANNEL_ID = "coinary_reminder_channel"
    const val CHANNEL_NAME = "Recordatorios Coinary"
    const val CHANNEL_DESCRIPTION = "Notificaciones para tus recordatorios financieros de Coinary"

    fun scheduleNotification(
        context: Context,
        dateTimeString: String,
        title: String,
        message: String
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date: Date? = dateFormat.parse(dateTimeString)

        date?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Permiso para alarmas exactas no concedido. Por favor, habilítelo en la configuración de la aplicación.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val notificationId = System.currentTimeMillis().toInt()

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notificationId", notificationId)
                putExtra("title", title)
                putExtra("message", message)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId, // Usa el ID único aquí
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    date.time,
                    pendingIntent
                )

                val reminder = ReminderItem(
                    id = notificationId.toLong(),
                    title = title,
                    message = message,
                    dateTime = dateTimeString
                )
                ReminderStorage.addReminder(context, reminder)

                Toast.makeText(context, "Recordatorio programado para el $dateTimeString", Toast.LENGTH_LONG).show()
            } catch (e: SecurityException) {
                Toast.makeText(context, "Error al programar el recordatorio: Permiso denegado o restricción del sistema.", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }

        } ?: run {
            Toast.makeText(context, "Formato de fecha/hora inválido.", Toast.LENGTH_SHORT).show()
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}