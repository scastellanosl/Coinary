package com.example.coinary.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.coinary.MainActivity
import com.example.coinary.R
import com.example.coinary.utils.NotificationScheduler

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val title = intent?.getStringExtra("title") ?: "Recordatorio Coinary"
            val message = intent?.getStringExtra("message") ?: "¡Es hora de revisar tus finanzas!"

            val resultIntent = Intent(context, MainActivity::class.java)
            val resultPendingIntent: PendingIntent? = PendingIntent.getActivity(
                context,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(it, NotificationScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)
            try {
                with(NotificationManagerCompat.from(it)) {
                    notify(System.currentTimeMillis().toInt(), builder.build())
                }
            } catch (e: SecurityException) {
                e.printStackTrace()

            }
        }
    }
}