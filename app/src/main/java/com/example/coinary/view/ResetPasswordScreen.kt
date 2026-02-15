package com.example.coinary.view

import android.util.Patterns
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coinary.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ResetPasswordScreen: UI for initiating the password recovery process.
 * Allows the user to enter their registered email address to receive a password reset link
 * via Firebase Authentication.
 *
 * @param onBackToLogin Callback to navigate back to the Login screen.
 */
@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    // --- State Management ---
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Snackbar state for user feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val InterFont = FontFamily.Default // Placeholder for custom font

    /**
     * Validates that the input is a correctly formatted email address.
     */
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && email.isNotBlank()
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
            // --- Header Section ---
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_image_register), // Reusing register image or specific reset image
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "Reset your password",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(100.dp), // Padding to position text over image
                    textAlign = TextAlign.Center,
                    fontFamily = InterFont
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Email Input Field ---
            TextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null // Clear error on type
                },
                label = {
                    Text(
                        text = "Email",
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
                    .border(1.dp, Color.White, shape = RoundedCornerShape(12.dp)),
                textStyle = TextStyle(
                    fontFamily = InterFont,
                    color = Color.White,
                    fontSize = 16.sp
                ),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    errorIndicatorColor = Color.Transparent
                ),
                isError = emailError != null
            )

            // Error Message Display
            emailError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(start = 10.dp, top = 4.dp),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Submit Button ---
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    emailError = null

                    if (!isValidEmail(trimmedEmail)) {
                        emailError = "Invalid email format"
                        return@Button
                    }

                    isLoading = true
                    auth.sendPasswordResetEmail(trimmedEmail)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Reset email sent. Check your inbox or spam folder.")
                                }
                                // Optional: Delay navigation slightly to let user read message
                                onBackToLogin()
                            } else {
                                val errorMsg = task.exception?.message ?: "Failed to send reset email."
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(errorMsg)
                                }
                            }
                        }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4D54BF),
                    disabledContainerColor = Color(0xFF4D54BF).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(45.dp)
                    .padding(horizontal = 20.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Send Email",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        fontFamily = InterFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Back to Login Link ---
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = Color(0xFF4D54BF), fontWeight = FontWeight.Bold)) {
                    append("Back to Login")
                }
            }

            ClickableText(
                text = annotatedText,
                modifier = Modifier.padding(4.dp),
                onClick = { onBackToLogin() },
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = InterFont,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}