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
import androidx.compose.ui.res.stringResource
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

/**
 * Screen for managing the user profile.
 * Allows the user to view and edit their personal information (name, profile picture),
 * configure notification settings, and sign out of the application.
 *
 * It uses SharedPreferences for local persistence of profile data to ensure
 * a seamless user experience even when offline or before Firebase syncs.
 */
@Composable
fun ProfileScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    // --- Screen Configuration & Context ---
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val screenWidthDp = configuration.screenWidthDp.toFloat()

    // --- System UI Controller ---
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black
    SideEffect {
        systemUiController.setStatusBarColor(color = statusBarColor, darkIcons = false)
    }

    // --- Authentication & User Data ---
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val user = googleAuthClient.getSignedInUser()
    val firebaseUser = remember { Firebase.auth.currentUser }

    // --- Notification Time Picker State ---
    var hour by remember { mutableStateOf(18) }
    var minute by remember { mutableStateOf(0) }

    val timePickerDialog = remember {
        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            hour = selectedHour
            minute = selectedMinute
        }, hour, minute, false)
    }

    // --- Local Persistence (SharedPreferences) ---
    val prefs = remember {
        context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE)
    }

    // --- Profile State ---
    // Load from SharedPreferences first for immediate UI response, fallback to Firebase.
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

    // Editing State
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(displayName) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // --- Image Picker Logic ---
    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    // Grant persistable URI permission to access the content even after a restart
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    photoUri = uri

                    // Save locally in SharedPreferences
                    prefs.edit()
                        .putString("photo_uri", uri.toString())
                        .apply()

                    // Sync with Firebase Auth
                    firebaseUser?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setPhotoUri(uri)
                            .build()
                    )?.addOnCompleteListener {
                        // Sync completed (optional handling)
                    }
                } catch (e: Exception) {
                    // Fallback in case of permission errors
                    photoUri = uri
                    prefs.edit()
                        .putString("photo_uri", uri.toString())
                        .apply()
                }
            }
        }
    )

    // Load localized error strings
    val errorInvalidName = stringResource(R.string.error_invalid_name)
    val errorEditLimit = stringResource(R.string.error_edit_limit)

    // --- Validation Logic ---

    /**
     * Validates the format of the user's name.
     * Rules: Length 3-20, allowed characters (Letters, accents), and banned words.
     */
    fun isNameValid(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length !in 3..20) return false
        val allowed = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñ ]+$")
        if (!allowed.matches(trimmed)) return false
        val banned = listOf("ads", "ad", "admin", "sex", "spam")
        return banned.none { it in trimmed.lowercase() }
    }

    /**
     * Checks if the user is allowed to edit their name based on the time cooldown (15 days).
     */
    fun canEditName(): Boolean {
        val now = System.currentTimeMillis()
        val days15 = 15L * 24 * 60 * 60 * 1000
        return now - lastEditMillis >= days15
    }

    /**
     * Saves the modified name to both local storage and Firebase.
     */
    fun saveName() {
        if (!isNameValid(nameDraft)) {
            errorMsg = errorInvalidName
            return
        }
        if (!canEditName()) {
            errorMsg = errorEditLimit
            return
        }
        displayName = nameDraft.trim()
        lastEditMillis = System.currentTimeMillis()
        errorMsg = null
        editingName = false

        // Update SharedPreferences
        prefs.edit()
            .putString("display_name", displayName)
            .putLong("last_name_edit", lastEditMillis)
            .apply()

        // Update Firebase Auth
        firebaseUser?.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
        )?.addOnCompleteListener { }
    }

    // --- UI Layout ---
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Top Bar
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

        // 2. Main Content
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.darker_background),
                contentDescription = stringResource(R.string.background_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // -- Avatar Section --
                Box(
                    modifier = Modifier
                        .offset(y = (screenHeight * 0.025f))
                        .fillMaxWidth(0.25f)
                        .aspectRatio(1f)
                ) {
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = stringResource(R.string.profile_pic_desc),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.user_icon),
                            contentDescription = stringResource(R.string.profile_pic_desc),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Edit Icon (Pencil)
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
                            stringResource(R.string.edit_photo_desc),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // -- Display Name Header --
                Text(
                    text = displayName.ifBlank { user?.username ?: stringResource(R.string.default_user_name) },
                    fontWeight = FontWeight.Bold,
                    fontSize = (screenWidthDp * 0.042f).sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = screenHeight * 0.032f)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // -- User Info Card --
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                        contentDescription = stringResource(R.string.card_background_desc),
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
                        // Title
                        Text(
                            text = stringResource(R.string.your_info),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = (screenWidthDp * 0.07f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Name Field
                        Text(
                            text = stringResource(R.string.name_label),
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
                                text = displayName.ifBlank { stringResource(R.string.default_user_name) },
                                color = Color.White,
                                fontWeight = FontWeight.Thin,
                                fontSize = (screenWidthDp * 0.050f).sp
                            )
                            IconButton(onClick = {
                                nameDraft = displayName
                                editingName = true
                                errorMsg = null
                            }) {
                                Icon(Icons.Default.Edit, stringResource(R.string.edit_name_desc), tint = Color.White)
                            }
                        }

                        // Editable TextField for Name
                        if (editingName) {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it },
                                label = { Text(stringResource(R.string.new_name_label), color = Color.White.copy(alpha = 0.7f)) },
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
                                    Text(stringResource(R.string.cancel_button), color = Color.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                Button(onClick = { saveName() }) {
                                    Text(stringResource(R.string.save_button))
                                }
                            }
                        }

                        // Error Message Display
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

                        // Email Field (Read-only)
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
                            text = user?.email ?: FirebaseAuth.getInstance().currentUser?.email ?: stringResource(R.string.default_email),
                            color = Color.White,
                            fontWeight = FontWeight.Thin,
                            fontSize = (screenWidthDp * 0.050f).sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = screenHeight * 0.004f)
                                .fillMaxWidth()
                        )

                        // Notification Time Picker
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
                            // Hour Box
                            Box(
                                modifier = Modifier
                                    .size(screenHeight * 0.06f)
                                    .clickable { timePickerDialog.show() }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_background),
                                    contentDescription = stringResource(R.string.hour_bg_desc),
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

                            // Minute Box
                            Box(
                                modifier = Modifier
                                    .size(screenHeight * 0.06f)
                                    .clickable { timePickerDialog.show() }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_background),
                                    contentDescription = stringResource(R.string.minute_bg_desc),
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

                            // AM/PM Indicator
                            Box(
                                modifier = Modifier
                                    .size(screenHeight * 0.06f)
                                    .clickable { timePickerDialog.show() },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.button_hour),
                                    contentDescription = stringResource(R.string.ampm_bg_desc),
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

                        // Data Protection Message
                        Text(
                            text = stringResource(R.string.data_protection_msg),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -- Logout Button --
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