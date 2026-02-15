package com.example.coinary

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.coinary.navigation.AppNavigation
import com.example.coinary.ui.theme.CoinaryTheme
import com.example.coinary.utils.NotificationScheduler

/**
 * MainActivity: The entry point of the application.
 * Responsible for initializing the UI content, setting up notification channels,
 * and requesting necessary system permissions (Notifications & Exact Alarms) upon launch.
 */
class MainActivity : ComponentActivity() {

    // Launcher for requesting runtime permissions (specifically POST_NOTIFICATIONS for Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted: The app can post notifications.
        } else {
            // Permission denied: The app should degrade gracefully (e.g., show a snackbar explaining why).
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Notification Channel immediately upon app startup
        NotificationScheduler.createNotificationChannel(applicationContext)

        // --- Permission Check: Notifications (Android 13 / API 33+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // --- Permission Check: Exact Alarms (Android 12 / API 31+) ---
        // Required for precise scheduling of reminders.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Note: It is best practice to show a dialog explaining WHY this permission is needed
                // before redirecting the user to Settings.
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        // --- UI Content Setup ---
        setContent {
            CoinaryTheme {
                AppNavigation()
            }
        }
    }
}