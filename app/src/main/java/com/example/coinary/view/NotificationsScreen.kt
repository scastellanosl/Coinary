package com.example.coinary.view

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.data.ReminderItem
import com.example.coinary.utils.NotificationScheduler
import com.example.coinary.utils.ReminderStorage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.res.stringResource

/**
 * NotificationsScreen: Displays a list of scheduled reminders.
 * Allows users to view details and delete pending notifications.
 *
 * @param navController Navigation controller.
 * @param onBackClick Callback for back navigation.
 * @param onLogout Callback for logout (optional).
 */
@Composable
fun NotificationsScreen(
    navController: NavController,
    onBackClick: () -> Unit = { },
    onLogout: () -> Unit = { }
) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black

    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    // Mutable list to hold the current reminders
    val reminders = remember { mutableStateListOf<ReminderItem>() }

    // Function to reload reminders from storage
    val loadReminders: () -> Unit = {
        reminders.clear()
        reminders.addAll(ReminderStorage.loadReminders(context))
    }

    // Initial load when entering the screen
    LaunchedEffect(Unit) {
        loadReminders()
    }

    /**
     * Handles the deletion logic for a reminder item.
     * 1. Cancels the system alarm.
     * 2. Removes data from local storage.
     * 3. Updates the UI list instantly.
     */
    val onDeleteReminder: (ReminderItem) -> Unit = { reminderToDelete ->
        NotificationScheduler.cancelNotification(context, reminderToDelete.id)
        ReminderStorage.removeReminder(context, reminderToDelete.id)
        reminders.remove(reminderToDelete)
        Toast.makeText(context, "Reminder deleted", Toast.LENGTH_SHORT).show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Header Section ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = stringResource(R.string.notifications),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // --- Content Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(R.drawable.fondo_contenedor_categoria),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .padding(top = 15.dp)
            ) {
                if (reminders.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_reminders_yet),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(reminders, key = { it.id }) { reminder ->
                            NotificationCard(
                                title = reminder.title,
                                message = "${reminder.message} - ${reminder.dateTime}",
                                onDeleteClick = { onDeleteReminder(reminder) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * NotificationCard: Reusable component for a single notification item.
 * Implements TextOverflow.Ellipsis to prevent layout breaking on long texts.
 */
@Composable
fun NotificationCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 3f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.notification_background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Column with Weight to occupy available space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp) // Padding to avoid touching the delete button
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1, // Limit title to 1 line
                    overflow = TextOverflow.Ellipsis // Add "..." if too long
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.Thin,
                    fontSize = 12.sp,
                    maxLines = 2, // Limit message to 2 lines to fit card height
                    overflow = TextOverflow.Ellipsis // Add "..." if too long
                )
            }

            // Delete Action
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Reminder",
                    tint = Color.Red
                )
            }
        }
    }
}