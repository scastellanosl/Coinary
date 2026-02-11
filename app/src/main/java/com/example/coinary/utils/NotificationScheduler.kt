package com.example.coinary.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.coinary.data.ReminderItem
import com.example.coinary.receivers.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationScheduler {

    const val CHANNEL_ID = "coinary_reminder_channel"
    const val CHANNEL_NAME = "Recordatorios Coinary"
    const val CHANNEL_DESCRIPTION = "Notificaciones financieras"

    // ID fijo para que el recordatorio diario se sobrescriba a sí mismo
    const val DAILY_REMINDER_ID = 7777

    // --- 1. AGENDAR RECORDATORIO ESPECÍFICO (FECHA/HORA) ---
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

            // Verificar permisos en Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Permiso para alarmas exactas no concedido.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Usamos el tiempo actual como ID único para recordatorios de lista
            val notificationId = System.currentTimeMillis().toInt()

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notificationId", notificationId)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("isDaily", false) // No es diario
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, date.time, pendingIntent)
                }

                // Guardar en Storage para mostrarlo en la lista
                val reminder = ReminderItem(
                    id = notificationId.toLong(),
                    title = title,
                    message = message,
                    dateTime = dateTimeString
                )
                ReminderStorage.addReminder(context, reminder)

                // Toast.makeText(context, "Programado para $dateTimeString", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(context, "Error de permisos.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "Formato de fecha inválido", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 2. AGENDAR RECORDATORIO DIARIO (PERFIL) ---
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Verificar permisos en Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        // Preparamos el Intent con la marca "isDaily"
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationId", DAILY_REMINDER_ID)
            putExtra("title", "¡Hola! \uD83D\uDC4B")
            putExtra("message", "Es hora de registrar tus gastos del día.")
            putExtra("isDaily", true) // ESTO ACTIVA EL BUCLE EN EL RECEIVER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Configuramos la fecha/hora
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, programamos para mañana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // --- 3. CANCELAR NOTIFICACIÓN (Esta es la que te faltaba) ---
    fun cancelNotification(context: Context, notificationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)

        // Es crucial convertir el Long a Int porque PendingIntent usa Int para el requestCode
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancela la alarma en el sistema
        alarmManager.cancel(pendingIntent)

        // Opcional: Cancelar la notificación visual si aún está en la barra de estado
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId.toInt())

        Toast.makeText(context, "Recordatorio cancelado", Toast.LENGTH_SHORT).show()
    }

    // --- 4. CREAR CANAL DE NOTIFICACIÓN ---
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}