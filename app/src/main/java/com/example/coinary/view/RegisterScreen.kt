package com.example.coinary.view

import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

/**
 * RegisterScreen: Provides the UI for creating a new user account.
 * Handles Email/Password registration validation and Google Sign-In integration.
 *
 * @param onRegisterSuccess Callback triggered upon successful account creation.
 * @param onLoginClick Callback to navigate to the Login screen.
 * @param googleAuthClient Client for handling Google Sign-In intent.
 * @param launcher Activity result launcher for the Google Sign-In process.
 */
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    googleAuthClient: GoogleAuthClient,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- Form State ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // --- Validation Error State ---
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // --- Loading State ---
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingGoogle by remember { mutableStateOf(false) }

    val InterFont = FontFamily.Default // Ideally, this should be your app's custom font family

    /**
     * Validates input fields and sets error messages.
     * @return True if all inputs are valid, false otherwise.
     */
    fun validateInputs(): Boolean {
        var valid = true
        emailError = null
        passwordError = null
        confirmPasswordError = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = context.getString(R.string.mail_error)
            valid = false
        }
        if (password.length < 6) {
            passwordError = context.getString(R.string.password_error)
            valid = false
        }
        if (password != confirmPassword) {
            confirmPasswordError = context.getString(R.string.passwords_not_match)
            valid = false
        }

        return valid
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header Image & Title ---
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_image_register),
                    contentDescription = "Register Illustration",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = stringResource(R.string.description),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(65.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(30.dp))

            // --- Email Input ---
            TextField(
                value = email,
                onValueChange = { email = it },
                label = {
                    Text(
                        text = stringResource(R.string.email),
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = Color(0xFF868686)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 20.dp)
                    .background(Color.Black)
                    .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp)),
                textStyle = TextStyle(
                    fontFamily = InterFont,
                    color = Color.White,
                    fontSize = 16.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                isError = emailError != null
            )
            emailError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 4.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Password Input ---
            TextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        text = stringResource(R.string.password_label),
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = Color(0xFF868686)
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 20.dp)
                    .background(Color.Black)
                    .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp)),
                textStyle = TextStyle(
                    fontFamily = InterFont,
                    color = Color.White,
                    fontSize = 16.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                isError = passwordError != null
            )
            passwordError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 4.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Confirm Password Input ---
            TextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = {
                    Text(
                        text = stringResource(R.string.confirm_password_label),
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp,
                        color = Color(0xFF868686)
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 20.dp)
                    .background(Color.Black)
                    .border(1.dp, color = Color.White, shape = RoundedCornerShape(12.dp)),
                textStyle = TextStyle(
                    fontFamily = InterFont,
                    color = Color.White,
                    fontSize = 16.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                isError = confirmPasswordError != null
            )
            confirmPasswordError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 40.dp, top = 4.dp, bottom = 4.dp).align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Register Button ---
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(vertical = 8.dp),
                    color = Color.White
                )
            } else {
                Button(
                    onClick = {
                        if (validateInputs()) {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(email.trim(), password.trim())
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onRegisterSuccess()
                                    } else {
                                        val errorMsg = task.exception?.message ?: "Registration failed"
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Error: $errorMsg")
                                        }
                                    }
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(45.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.create_account),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        fontFamily = InterFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Already have account? Login Link ---
            val annotatedText = buildAnnotatedString {
                append(stringResource(R.string.already_account))
                pushStringAnnotation(tag = "LOGIN", annotation = "login")
                withStyle(style = SpanStyle(color = Color(0xFF4D54BF), fontWeight = FontWeight.Bold)) {
                    append(stringResource(R.string.login))
                }
                pop()
            }

            ClickableText(
                text = annotatedText,
                modifier = Modifier.padding(4.dp),
                onClick = { offset ->
                    annotatedText.getStringAnnotations(tag = "LOGIN", start = offset, end = offset)
                        .firstOrNull()?.let {
                            onLoginClick()
                        }
                },
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White,
                    fontFamily = InterFont
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Google Sign In Button ---
            if (isLoadingGoogle) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp).padding(vertical = 8.dp),
                    color = Color.White)
            } else {
                Button(
                    onClick = {
                        isLoadingGoogle = true
                        launcher.launch(googleAuthClient.getSignInIntent())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(45.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google logo",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.google_sign_in),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = InterFont
                    )
                }
            }
        }
    }
}