package com.example.coinary.view

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.example.coinary.utils.NotificationScheduler // Importamos tu Scheduler existente
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black

    SideEffect {
        systemUiController.setStatusBarColor(color = statusBarColor, darkIcons = false)
    }

    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val user = googleAuthClient.getSignedInUser()
    val firebaseUser = remember { Firebase.auth.currentUser }

    // --- Preferencias para guardar la hora ---
    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }

    // Cargar hora guardada (o por defecto las 18:00)
    var hour by remember { mutableStateOf(prefs.getInt("daily_hour", 18)) }
    var minute by remember { mutableStateOf(prefs.getInt("daily_minute", 0)) }

    // --- Permisos de Notificación (Android 13+) ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Si aceptó, programamos
                NotificationScheduler.scheduleDailyReminder(context, hour, minute)
                Toast.makeText(context, "Recordatorio diario activado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Crear el canal de notificaciones al iniciar la pantalla
    LaunchedEffect(Unit) {
        NotificationScheduler.createNotificationChannel(context)
    }

    // --- Selector de Hora ---
    val timePickerDialog = remember {
        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            hour = selectedHour
            minute = selectedMinute

            // 1. Guardar en SharedPreferences (Vital para que el Receiver reprogramé mañana)
            prefs.edit()
                .putInt("daily_hour", selectedHour)
                .putInt("daily_minute", selectedMinute)
                .apply()

            // 2. Verificar Permisos y Programar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    NotificationScheduler.scheduleDailyReminder(context, selectedHour, selectedMinute)
                    Toast.makeText(context, "Recordatorio actualizado a las %02d:%02d".format(selectedHour, selectedMinute), Toast.LENGTH_SHORT).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                NotificationScheduler.scheduleDailyReminder(context, selectedHour, selectedMinute)
                Toast.makeText(context, "Recordatorio actualizado a las %02d:%02d".format(selectedHour, selectedMinute), Toast.LENGTH_SHORT).show()
            }

        }, hour, minute, false) // false = formato 12h (AM/PM)
    }

    // --- Lógica del Perfil (Nombre/Foto) ---
    var displayName by remember { mutableStateOf(prefs.getString("display_name", null) ?: firebaseUser?.displayName ?: "") }
    var photoUri by remember { mutableStateOf(prefs.getString("photo_uri", null)?.toUri() ?: firebaseUser?.photoUrl?.toString()?.toUri()) }
    var lastEditMillis by remember { mutableStateOf(prefs.getLong("last_name_edit", 0L)) }
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(displayName) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    photoUri = uri
                    prefs.edit().putString("photo_uri", uri.toString()).apply()
                    firebaseUser?.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(uri).build())
                } catch (e: Exception) {
                    photoUri = uri
                    prefs.edit().putString("photo_uri", uri.toString()).apply()
                }
            }
        }
    )

    fun isNameValid(name: String) = name.trim().length in 3..20 && Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$").matches(name.trim())
    fun canEditName() = System.currentTimeMillis() - lastEditMillis >= (15L * 24 * 60 * 60 * 1000)

    fun saveName() {
        if (!isNameValid(nameDraft)) { errorMsg = "Nombre inválido"; return }
        if (!canEditName()) { errorMsg = "Espera 15 días para cambiar el nombre"; return }
        displayName = nameDraft.trim()
        lastEditMillis = System.currentTimeMillis()
        errorMsg = null
        editingName = false
        prefs.edit().putString("display_name", displayName).putLong("last_name_edit", lastEditMillis).apply()
        firebaseUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build())
    }

    // --- UI Layout ---
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight
        val scrollState = rememberScrollState()

        Image(
            painter = painterResource(id = R.drawable.darker_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Text(text = stringResource(R.string.profile_configuration), color = Color.White, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(maxHeight * 0.02f))

                // Avatar
                Box(modifier = Modifier.size((maxWidth * 0.25f).coerceIn(100.dp, 200.dp))) {
                    val mod = Modifier.fillMaxSize().clip(CircleShape)
                    if (photoUri != null) AsyncImage(model = photoUri, contentDescription = null, modifier = mod, contentScale = ContentScale.Crop)
                    else Image(painter = painterResource(id = R.drawable.user_icon), contentDescription = null, modifier = mod, contentScale = ContentScale.Crop)

                    IconButton(
                        onClick = { pickPhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x=4.dp, y=4.dp).background(Color.Black.copy(0.5f), CircleShape).size(36.dp)
                    ) { Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(text = displayName.ifBlank { user?.username ?: stringResource(R.string.default_user_name) }, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                Box(modifier = Modifier.fillMaxWidth(0.95f)) {
                    Image(painter = painterResource(id = R.drawable.fondo_contenedor_categoria), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = stringResource(R.string.your_info), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(text = stringResource(R.string.name_label), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = displayName, color = Color.White, fontWeight = FontWeight.Thin, fontSize = 16.sp)
                            IconButton(onClick = { nameDraft = displayName; editingName = true; errorMsg = null }) { Icon(Icons.Default.Edit, null, tint = Color.White) }
                        }
                        if (editingName) {
                            OutlinedTextField(
                                value = nameDraft, onValueChange = { nameDraft = it },
                                label = { Text(stringResource(R.string.new_name_label), color = Color.White) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White)
                            )
                            Row(modifier = Modifier.padding(top=8.dp)) {
                                TextButton(onClick = { editingName = false }) { Text(stringResource(R.string.cancel_button), color = Color.White) }
                                Button(onClick = { saveName() }) { Text(stringResource(R.string.save_button)) }
                            }
                        }
                        if (errorMsg != null) Text(text = errorMsg!!, color = Color.Red, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = stringResource(R.string.email), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = user?.email ?: "", color = Color.White, fontWeight = FontWeight.Thin, fontSize = 16.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- TIME PICKER DISPLAY ---
                        Text(text = stringResource(R.string.hour), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            val boxSize = 50.dp
                            // Hora
                            Box(modifier = Modifier.size(boxSize).clickable { timePickerDialog.show() }, contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.button_background), null, modifier = Modifier.fillMaxSize())
                                Text(text = "%02d".format(if (hour % 12 == 0) 12 else hour % 12), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(" : ", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            // Minutos
                            Box(modifier = Modifier.size(boxSize).clickable { timePickerDialog.show() }, contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.button_background), null, modifier = Modifier.fillMaxSize())
                                Text(text = "%02d".format(minute), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            // AM/PM
                            Box(modifier = Modifier.size(boxSize).clickable { timePickerDialog.show() }, contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.button_hour), null, modifier = Modifier.fillMaxSize())
                                Text(text = if (hour < 12) "AM" else "PM", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = stringResource(R.string.data_protection_msg), color = Color.White.copy(0.8f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { coroutineScope.launch { googleAuthClient.signOut(); onLogout() } },
                    modifier = Modifier.fillMaxWidth(0.85f).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBF4D4D), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(text = stringResource(R.string.logout), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}