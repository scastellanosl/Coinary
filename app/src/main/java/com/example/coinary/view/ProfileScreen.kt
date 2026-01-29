package com.example.coinary.view

import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()

    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black
    SideEffect {
        systemUiController.setStatusBarColor(color = statusBarColor, darkIcons = false)
    }

    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val user = googleAuthClient.getSignedInUser()

    var hour by remember { mutableStateOf(18) }
    var minute by remember { mutableStateOf(0) }

    val timePickerDialog = remember {
        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            hour = selectedHour
            minute = selectedMinute
        }, hour, minute, false)
    }

    // SharedPreferences para persistir datos localmente
    val prefs = remember {
        context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE)
    }

    // Estados para perfil
    val firebaseUser = remember { Firebase.auth.currentUser }

    // Cargar desde SharedPreferences primero, luego desde Firebase
    var displayName by remember {
        mutableStateOf(
            prefs.getString("display_name", null)
                ?: firebaseUser?.displayName
                ?: ""
        )
    }

    var photoUri by remember {
        mutableStateOf(
            prefs.getString("photo_uri", null)?.toUri()
                ?: firebaseUser?.photoUrl?.toString()?.toUri()
        )
    }

    var lastEditMillis by remember {
        mutableStateOf(prefs.getLong("last_name_edit", 0L))
    }

    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(displayName) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    // Dar permisos permanentes a la URI
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    photoUri = uri

                    // Guardar en SharedPreferences para persistencia local
                    prefs.edit()
                        .putString("photo_uri", uri.toString())
                        .apply()

                    // Guardar en Firebase Auth para sincronización
                    firebaseUser?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build()
                    )?.addOnCompleteListener {
                        // Sincronizado
                    }
                } catch (e: Exception) {
                    // En caso de error con permisos persistentes
                    photoUri = uri
                    prefs.edit()
                        .putString("photo_uri", uri.toString())
                        .apply()
                }
            }
        }
    )

    // Validaciones
    fun isNameValid(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length !in 3..20) return false
        val allowed = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$")
        if (!allowed.matches(trimmed)) return false
        val banned = listOf("ads", "ad", "admin", "sex", "spam")
        return banned.none { it in trimmed.lowercase() }
    }

    fun canEditName(): Boolean {
        val now = System.currentTimeMillis()
        val days15 = 15L * 24 * 60 * 60 * 1000
        return now - lastEditMillis >= days15
    }

    fun saveName() {
        if (!isNameValid(nameDraft)) {
            errorMsg = "Nombre inválido: solo letras/espacios, 3-20 caracteres, sin palabras prohibidas."
            return
        }
        if (!canEditName()) {
            errorMsg = "Solo puedes cambiar el nombre una vez cada 15 días."
            return
        }
        displayName = nameDraft.trim()
        lastEditMillis = System.currentTimeMillis()
        errorMsg = null
        editingName = false

        // Guardar en SharedPreferences
        prefs.edit()
            .putString("display_name", displayName)
            .putLong("last_name_edit", lastEditMillis)
            .apply()

        // Guardar en Firebase Auth
        firebaseUser?.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
        )?.addOnCompleteListener { }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                text = stringResource(R.string.profile_configuration),
                color = Color.White
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 3.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        Icons.Default.Notifications,
                        "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(35.dp)
                    )
                }
            }
        }

        // Contenido principal
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.darker_background),
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Avatar editable CIRCULAR
                Box(
                    modifier = Modifier
                        .offset(y = (screenHeight * 0.025f))
                        .fillMaxWidth(0.25f)
                        .aspectRatio(1f)
                ) {
                    // Mostrar imagen circular
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.user_icon),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Lapicito movido más a la derecha
                    IconButton(
                        onClick = {
                            pickPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            "Editar foto",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Nombre del usuario
                Text(
                    text = displayName.ifBlank { user?.username ?: "User" },
                    fontWeight = FontWeight.Bold,
                    fontSize = (screenWidthDp * 0.042f).sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = screenHeight * 0.032f)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Your Info Card
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                        contentDescription = "Card background",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenWidth * 0.048f)
                            .padding(top = screenHeight * 0.012f),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenWidth * 0.048f)
                            .padding(top = screenHeight * 0.038f)
                    ) {
                        // Your Info
                        Text(
                            text = stringResource(R.string.your_info),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (screenWidthDp * 0.07f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Name editable
                        Text(
                            text = "Nombre",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (screenWidthDp * 0.055f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = screenHeight * 0.02f)
                                .fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName.ifBlank { "Usuario" },
                                color = Color.White,
                                fontWeight = FontWeight.Thin,
                                fontSize = (screenWidthDp * 0.050f).sp
                            )
                            IconButton(onClick = {
                                nameDraft = displayName
                                editingName = true
                                errorMsg = null
                            }) {
                                Icon(Icons.Default.Edit, "Editar nombre", tint = Color.White)
                            }
                        }

                        if (editingName) {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it },
                                label = { Text("Nuevo nombre", color = Color.White.copy(alpha = 0.7f)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    cursorColor = Color.White
                                )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(onClick = { editingName = false }) {
                                    Text("Cancelar", color = Color.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                Button(onClick = { saveName() }) {
                                    Text("Guardar")
                                }
                            }
                        }

                        if (errorMsg != null) {
                            Text(
                                text = errorMsg!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        // Email
                        Text(
                            text = stringResource(R.string.email),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (screenWidthDp * 0.055f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = screenHeight * 0.02f)
                                .fillMaxWidth()
                        )
                        Text(
                            text = user?.email ?: FirebaseAuth.getInstance().currentUser?.email ?: "mailusuario",
                            color = Color.White,
                            fontWeight = FontWeight.Thin,
                            fontSize = (screenWidthDp * 0.050f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = screenHeight * 0.004f)
                                .fillMaxWidth()
                        )

                        // Hora de notificación
                        Text(
                            text = stringResource(R.string.hour),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (screenWidthDp * 0.048f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = screenHeight * 0.02f)
                                .fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = (screenHeightDp * 0.01f).dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(screenHeight * 0.06f)
                                    .clickable { timePickerDialog.show() }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_background),
                                    contentDescription = "Hour background",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "%02d".format(if (hour % 12 == 0) 12 else hour % 12),
                                    color = Color.White,
                                    fontSize = (screenWidthDp * 0.052f).sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Text(
                                text = ":",
                                color = Color.White,
                                fontSize = (screenWidthDp * 0.105f).sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(screenHeight * 0.06f)
                                    .clickable { timePickerDialog.show() }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_background),
                                    contentDescription = "Minute background",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = "%02d".format(minute),
                                    color = Color.White,
                                    fontSize = (screenWidthDp * 0.052f).sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Spacer(modifier = Modifier.width((screenWidth * 0.022f)))

                            Box(
                                modifier = Modifier
                                    .size(screenHeight * 0.06f)
                                    .clickable { timePickerDialog.show() },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_hour),
                                    contentDescription = "AM/PM background",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    text = if (hour < 12) "AM" else "PM",
                                    color = Color.White,
                                    fontSize = (screenWidthDp * 0.052f).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mensaje de protección
                        Text(
                            text = "Tus datos están protegidos y sincronizados con Firebase.",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Botón logout
                Button(
                    onClick = {
                        coroutineScope.launch {
                            googleAuthClient.signOut()
                            onLogout()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFBF4D4D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.logout),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

