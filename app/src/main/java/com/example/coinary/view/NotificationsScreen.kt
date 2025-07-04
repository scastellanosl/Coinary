package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.data.ReminderItem
import com.example.coinary.utils.ReminderStorage
import com.example.coinary.utils.NotificationScheduler
import com.google.accompanist.systemuicontroller.rememberSystemUiController

import androidx.compose.material.icons.filled.Delete
import android.widget.Toast

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

    val reminders = remember { mutableStateListOf<ReminderItem>() }

    // Función para recargar los recordatorios
    val loadReminders: () -> Unit = {
        reminders.clear()
        reminders.addAll(ReminderStorage.loadReminders(context))
    }

    // Cargar los recordatorios cuando el composable entra en la composición
    LaunchedEffect(Unit) {
        loadReminders()
    }

    // ************ NUEVO: Función para manejar el borrado de recordatorios ************
    val onDeleteReminder: (ReminderItem) -> Unit = { reminderToDelete ->
        // 1. Cancelar la alarma del sistema
        NotificationScheduler.cancelNotification(context, reminderToDelete.id)

        // 2. Eliminar el recordatorio de SharedPreferences
        ReminderStorage.removeReminder(context, reminderToDelete.id)

        // 3. Actualizar la lista en la UI
        reminders.remove(reminderToDelete)

        Toast.makeText(context, "Recordatorio eliminado: ${reminderToDelete.title}", Toast.LENGTH_SHORT).show()
    }
    // *********************************************************************************

    Column(modifier = Modifier.fillMaxSize()) {
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
            Text(text = context.getString(R.string.notifications), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(R.drawable.fondo_contenedor_categoria),
                contentDescription = "Notification background",
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
                            text = context.getString(R.string.no_reminders_yet),
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
 * composable reutilizable para una tarjeta de notificación individual
 */
@Composable
fun NotificationCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.Thin,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar recordatorio",
                    tint = Color.Red
                )
            }

        }
    }
}