package com.example.coinary.utils

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.coinary.R
import com.example.coinary.data.ReminderItem
import com.example.coinary.receivers.NotificationReceiver
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * NotificationScheduler
 *
 * Utility singleton responsible for scheduling, managing, and canceling local notifications.
 * Fully localized using strings.xml resources.
 */
object NotificationScheduler {

    const val CHANNEL_ID = "coinary_reminder_channel"
    const val DAILY_REMINDER_ID = 7777

    // ============================================================================================
    // REGION: SPECIFIC ONE-TIME NOTIFICATIONS
    // ============================================================================================

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
                    Toast.makeText(context, context.getString(R.string.toast_permission_alarm_needed), Toast.LENGTH_LONG).show()
                    return
                }
            }

            val notificationId = System.currentTimeMillis().toInt()

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("notificationId", notificationId)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("isDaily", false)
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

                val reminder = ReminderItem(
                    id = notificationId.toLong(),
                    title = title,
                    message = message,
                    dateTime = dateTimeString
                )
                ReminderStorage.addReminder(context, reminder)

            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.toast_permission_error), Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, context.getString(R.string.toast_invalid_date), Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================================================
    // REGION: DAILY RECURRING NOTIFICATIONS
    // ============================================================================================

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        // Usamos recursos de strings.xml
        val title = context.getString(R.string.notif_daily_title)
        val message = context.getString(R.string.notif_daily_message)

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationId", DAILY_REMINDER_ID)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("isDaily", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

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

    // ============================================================================================
    // REGION: CANCELLATION LOGIC
    // ============================================================================================

    fun cancelNotification(context: Context, notificationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId.toInt())

        Toast.makeText(context, context.getString(R.string.toast_reminder_canceled), Toast.LENGTH_SHORT).show()
    }

    // ============================================================================================
    // REGION: CHANNEL SETUP
    // ============================================================================================

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Obtenemos el nombre y descripción desde strings.xml para que cambie según el idioma del teléfono
            val name = context.getString(R.string.channel_name)
            val descriptionText = context.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ============================================================================================
    // REGION: DEBT & GOAL AUTOMATION
    // ============================================================================================

    fun scheduleDebtReminders(
        context: Context,
        debtId: String,
        debtDescription: String,
        dueDate: Date
    ) {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        val daysBeforeList = listOf(15, 8, 1)

        daysBeforeList.forEach { daysBefore ->
            calendar.time = dueDate
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -daysBefore)

            if (calendar.timeInMillis > currentTime) {
                val notificationId = generateDebtNotificationId(debtId, daysBefore)

                // Selección de título localizado
                val title = when (daysBefore) {
                    1 -> context.getString(R.string.notif_debt_1_day_title)
                    8 -> context.getString(R.string.notif_debt_8_days_title)
                    15 -> context.getString(R.string.notif_debt_15_days_title)
                    else -> context.getString(R.string.notif_debt_default_title)
                }

                // Construcción del mensaje usando String Templates (%1$s, %2$d)
                val daysString = if (daysBefore == 1) context.getString(R.string.day_singular) else context.getString(R.string.day_plural)
                val message = context.getString(R.string.notif_debt_body, debtDescription, daysBefore, daysString)

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
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                    }

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val reminder = ReminderItem(
                        id = notificationId.toLong(),
                        title = title,
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
            ReminderStorage.removeReminder(context, notificationId.toLong())
        }
    }

    fun showGoalCompletedNotification(
        context: Context,
        goalId: String,
        goalName: String,
        targetAmount: Double
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context)

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
        val formattedAmount = currencyFormat.format(targetAmount)

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

        // Strings localizados
        val title = context.getString(R.string.notif_goal_title)
        val message = context.getString(R.string.notif_goal_message, goalName, formattedAmount)
        val contentText = context.getString(R.string.notif_goal_short, goalName)

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
        }

        notificationManager.notify(goalId.hashCode(), notificationBuilder.build())

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDateTime = dateFormat.format(Date())

        val reminder = ReminderItem(
            id = System.currentTimeMillis(),
            title = title,
            message = message,
            dateTime = currentDateTime
        )
        ReminderStorage.addReminder(context, reminder)
    }

    private fun generateDebtNotificationId(debtId: String, daysBefore: Int): Int {
        return (debtId.hashCode() + daysBefore * 1000)
    }
}