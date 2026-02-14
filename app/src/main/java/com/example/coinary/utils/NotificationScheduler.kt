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

    // Agregar estas funciones al final del object NotificationScheduler

    // --- 5. AGENDAR RECORDATORIOS DE DEUDA (15, 8, 1 día antes) ---
    fun scheduleDebtReminders(
        context: Context,
        debtId: String,
        debtDescription: String,
        dueDate: Date
    ) {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()

        // Lista de días antes: [15, 8, 1]
        val daysBeforeList = listOf(15, 8, 1)

        daysBeforeList.forEach { daysBefore ->
            calendar.time = dueDate
            calendar.set(Calendar.HOUR_OF_DAY, 9) // 9:00 AM
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -daysBefore)

            // Solo programar si la fecha es futura
            if (calendar.timeInMillis > currentTime) {
                val notificationId = generateDebtNotificationId(debtId, daysBefore)

                val title = when (daysBefore) {
                    1 -> " Tu deuda vence mañana"
                    8 -> "️ Tu deuda vence pronto en un plazo de 8 días"
                    15 -> " Recordatorio de deuda dentro de 15 días"
                    else -> " Recordatorio de deuda"
                }

                val message = "Deuda: $debtDescription\nVence en $daysBefore ${if (daysBefore == 1) "día" else "días"}"

                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("notificationId", notificationId)
                    putExtra("title", title)
                    putExtra("message", message)
                    putExtra("isDaily", false)
                    putExtra("debtId", debtId)
                    putExtra("daysBefore", daysBefore)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

                    // Guardar en Storage
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val reminder = ReminderItem(
                        id = notificationId.toLong(),
                        title = title as String,
                        message = message,
                        dateTime = dateFormat.format(calendar.time)
                    )
                    ReminderStorage.addReminder(context, reminder)

                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- 6. CANCELAR RECORDATORIOS DE UNA DEUDA ---
    fun cancelDebtReminders(context: Context, debtId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val daysBeforeList = listOf(15, 8, 1)

        daysBeforeList.forEach { daysBefore ->
            val notificationId = generateDebtNotificationId(debtId, daysBefore)

            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            // Remover del Storage
            ReminderStorage.removeReminder(context, notificationId.toLong())
        }
    }

    // --- 7. NOTIFICACIÓN INMEDIATA AL COMPLETAR META ---
    fun showGoalCompletedNotification(
        context: Context,
        goalId: String,
        goalName: String,
        targetAmount: Double
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si no existe
        createNotificationChannel(context)

        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        val formattedAmount = currencyFormat.format(targetAmount).replace("COP", "").trim()

        val intent = Intent(context, com.example.coinary.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "ahorros")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            goalId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = "Meta completada"
        val message = "Felicitaciones Has completado tu meta de ahorro \"$goalName\" alcanzando $formattedAmount. ¡Sigue así! "

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Has alcanzado tu meta: $goalName")
                .setStyle(
                    android.app.Notification.BigTextStyle()
                        .bigText(message)
                )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
                .setContentTitle(title)
                .setContentText("Has alcanzado tu meta: $goalName")
                .setStyle(
                    android.app.Notification.BigTextStyle()
                        .bigText(message)
                )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        }

        notificationManager.notify(goalId.hashCode(), notification)

        //  GUARDAR EN EL HISTORIAL DE NOTIFICACIONES
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())

        val reminder = com.example.coinary.data.ReminderItem(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            dateTime = currentDateTime
        )
        ReminderStorage.addReminder(context, reminder)
    }

    // --- 8. GENERAR ID ÚNICO PARA NOTIFICACIONES DE DEUDA ---
    private fun generateDebtNotificationId(debtId: String, daysBefore: Int): Int {
        // Genera un ID único combinando el hash del debtId con el número de días
        return (debtId.hashCode() + daysBefore * 1000)
    }

}