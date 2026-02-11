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

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            // 1. Extraer datos del Intent
            val title = intent?.getStringExtra("title") ?: "Recordatorio Coinary"
            val message = intent?.getStringExtra("message") ?: "¡Es hora de revisar tus finanzas!"
            val isDaily = intent?.getBooleanExtra("isDaily", false) ?: false

            // ID fijo para diario (8888), aleatorio para otros
            val notificationId = if (isDaily) 8888 else intent?.getIntExtra("notificationId", System.currentTimeMillis().toInt()) ?: 0

            // 2. Asegurar que el canal existe
            NotificationScheduler.createNotificationChannel(ctx)

            // 3. Preparar acción al tocar (Abrir App)
            val resultIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val resultPendingIntent: PendingIntent? = PendingIntent.getActivity(
                ctx,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // --- ESTÉTICA DE LA NOTIFICACIÓN ---

            // A. Icono Grande (A la derecha): Usamos tu logo original a color
            val appLogoBitmap = BitmapFactory.decodeResource(ctx.resources, R.mipmap.ic_launcher)

            val builder = NotificationCompat.Builder(ctx, NotificationScheduler.CHANNEL_ID)
                // B. Icono Pequeño (Barra de estado): TU XML VECTORIAL
                .setSmallIcon(R.drawable.ic_notification)

                // C. Color de acento: Pinta tu icono ic_notification (ej: Azul Coinary o Dorado)
                .setColor(Color.parseColor("#4C6EF5"))

                // D. Asignar el icono grande a color
                .setLargeIcon(appLogoBitmap)

                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)

            // 4. Mostrar
            try {
                with(NotificationManagerCompat.from(ctx)) {
                    notify(notificationId, builder.build())
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            // 5. Bucle Diario (Reprogramar para mañana)
            if (isDaily) {
                val prefs = ctx.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
                val hour = prefs.getInt("daily_hour", 18)
                val minute = prefs.getInt("daily_minute", 0)

                NotificationScheduler.scheduleDailyReminder(ctx, hour, minute)
            }
        }
    }
}