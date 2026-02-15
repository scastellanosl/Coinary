package com.example.coinary.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.coinary.MainActivity
import com.example.coinary.R
import com.example.coinary.utils.NotificationScheduler
import com.example.coinary.utils.ReminderStorage

/**
 * NotificationReceiver: Handles system broadcasts to trigger financial notifications.
 * This receiver manages daily reminders, debt deadlines, and savings goal alerts,
 * applying specific visual styles and behaviors based on the notification type.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            // ====================================================================================
            // REGION: DATA EXTRACTION
            // ====================================================================================

            val title = intent?.getStringExtra("title") ?: "Coinary Reminder"
            val message = intent?.getStringExtra("message") ?: "It's time to check your finances!"
            val isDaily = intent?.getBooleanExtra("isDaily", false) ?: false

            // Context-specific IDs for navigation and logic
            val debtId = intent?.getStringExtra("debtId")
            val goalId = intent?.getStringExtra("goalId")
            val daysBefore = intent?.getIntExtra("daysBefore", -1) ?: -1

            /** * ID Generation: Fixed ID for daily reminders to prevent stacking (replaces old),
             * unique ID for specific debts or goals.
             */
            val notificationId = if (isDaily) {
                DAILY_NOTIFICATION_ID
            } else {
                intent?.getIntExtra("notificationId", System.currentTimeMillis().toInt()) ?: 0
            }

            // Ensure the notification channel is registered with the system
            NotificationScheduler.createNotificationChannel(ctx)

            // ====================================================================================
            // REGION: INTENT & NAVIGATION PREPARATION
            // ====================================================================================

            val resultIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // Route the user to the specific module based on notification type
                when {
                    debtId != null -> putExtra("navigate_to", "debts")
                    goalId != null -> putExtra("navigate_to", "ahorros")
                }
            }

            val resultPendingIntent: PendingIntent = PendingIntent.getActivity(
                ctx,
                notificationId, // Use notificationId to keep PendingIntents unique
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // ====================================================================================
            // REGION: NOTIFICATION AESTHETICS & FEEDBACK
            // ====================================================================================

            val appLogoBitmap = BitmapFactory.decodeResource(ctx.resources, R.mipmap.ic_launcher)

            /**
             * Semantic Coloring:
             * - Orange/Red: Urgent financial obligations (Debts).
             * - Green: Positive financial growth (Savings Goals).
             * - Blue: General application reminders.
             */
            val accentColor = when {
                debtId != null -> Color.parseColor("#FF5722")
                goalId != null -> Color.parseColor("#4CAF50")
                else -> Color.parseColor("#4C6EF5")
            }

            val builder = NotificationCompat.Builder(ctx, NotificationScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Vectorial icon for status bar
                .setColor(accentColor) // Tint for the small icon and notification name
                .setLargeIcon(appLogoBitmap) // Full-color app logo for the expanded view
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Support for long messages
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)

            /**
             * CRITICAL ALERT: Apply a specialized vibration pattern for urgent debts
             * (triggered 1 day before the deadline).
             */
            if (debtId != null && daysBefore == 1) {
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
            }

            // ====================================================================================
            // REGION: EXECUTION & POST-PROCESSING
            // ====================================================================================

            try {
                with(NotificationManagerCompat.from(ctx)) {
                    notify(notificationId, builder.build())
                }
            } catch (e: SecurityException) {
                // Fails silently if notification permissions are revoked;
                // system-level errors are handled by Android.
            }

            // Cleanup: Remote one-time notifications from local storage to keep data fresh
            if (!isDaily) {
                ReminderStorage.removeReminder(ctx, notificationId.toLong())
            }

            // Persistence: Reschedule the daily reminder for the next day
            if (isDaily) {
                val prefs = ctx.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
                val hour = prefs.getInt("daily_hour", 18)
                val minute = prefs.getInt("daily_minute", 0)

                NotificationScheduler.scheduleDailyReminder(ctx, hour, minute)
            }
        }
    }

    companion object {
        /** Constant ID for daily reminders to ensure they overwrite each other. */
        private const val DAILY_NOTIFICATION_ID = 8888
    }
}