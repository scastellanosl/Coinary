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

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let { ctx ->
            // 1. Extraer datos del Intent
            val title = intent?.getStringExtra("title") ?: "Recordatorio Coinary"
            val message = intent?.getStringExtra("message") ?: "¡Es hora de revisar tus finanzas!"
            val isDaily = intent?.getBooleanExtra("isDaily", false) ?: false

            // NUEVO: Extraer datos de deuda/meta
            val debtId = intent?.getStringExtra("debtId")
            val goalId = intent?.getStringExtra("goalId")
            val daysBefore = intent?.getIntExtra("daysBefore", -1) ?: -1

            // ID fijo para diario (8888), aleatorio para otros
            val notificationId = if (isDaily) {
                8888
            } else {
                intent?.getIntExtra("notificationId", System.currentTimeMillis().toInt()) ?: 0
            }

            // 2. Asegurar que el canal existe
            NotificationScheduler.createNotificationChannel(ctx)

            // 3. Preparar acción al tocar (Abrir App)
            val resultIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                // NUEVO: Si es de deuda o meta, navegar a esa sección
                when {
                    debtId != null -> putExtra("navigate_to", "debts")
                    goalId != null -> putExtra("navigate_to", "ahorros")
                }
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

            // NUEVO: Determinar el color según el tipo de notificación
            val notificationColor = when {
                debtId != null -> Color.parseColor("#FF5722") // Rojo/Naranja para deudas
                goalId != null -> Color.parseColor("#4CAF50") // Verde para metas
                else -> Color.parseColor("#4C6EF5") // Azul por defecto
            }

            val builder = NotificationCompat.Builder(ctx, NotificationScheduler.CHANNEL_ID)
                // B. Icono Pequeño (Barra de estado): TU XML VECTORIAL
                .setSmallIcon(R.drawable.ic_notification)

                // C. Color de acento: Pinta tu icono ic_notification
                .setColor(notificationColor)

                // D. Asignar el icono grande a color
                .setLargeIcon(appLogoBitmap)

                .setContentTitle(title)
                .setContentText(message)

                // NUEVO: Para mensajes largos, usar BigTextStyle
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true)

            // NUEVO: Agregar vibración especial para deudas urgentes (1 día antes)
            if (debtId != null && daysBefore == 1) {
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
            }

            // 4. Mostrar
            try {
                with(NotificationManagerCompat.from(ctx)) {
                    notify(notificationId, builder.build())
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            // 5. Gestión Post-Notificación

            // Si NO es diario y NO es de deuda/meta, eliminar de la lista
            if (!isDaily && debtId == null && goalId == null) {
                ReminderStorage.removeReminder(ctx, notificationId.toLong())
            }

            // Si es de deuda, eliminar del storage después de mostrar
            if (debtId != null) {
                ReminderStorage.removeReminder(ctx, notificationId.toLong())
            }

            // Si ES diario, reprogramar para mañana
            if (isDaily) {
                val prefs = ctx.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
                val hour = prefs.getInt("daily_hour", 18)
                val minute = prefs.getInt("daily_minute", 0)

                NotificationScheduler.scheduleDailyReminder(ctx, hour, minute)
            }
        }
    }
}
