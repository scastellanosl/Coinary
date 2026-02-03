@file:Suppress("DEPRECATION")

package com.example.coinary.view

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// Define global font family
val InterFont = FontFamily(Font(R.font.inter))

/**
 * The main login screen composable.
 * Handles Email/Password authentication and Google Sign-In via [GoogleAuthClient].
 *
 * @param onLoginSuccess Callback triggered when authentication is successful.
 * @param onNavigateToRegister Callback to navigate to the registration screen.
 * @param onForgotPasswordClick Callback to navigate to the password reset screen.
 */
@Composable
fun GoogleLoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Check if user is already signed in when the screen launches
    LaunchedEffect(Unit) {
        if (googleAuthClient.getSignedInUser() != null) {
            onLoginSuccess()
        }
    }

    // --- Google Sign-In Launcher ---
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        coroutineScope.launch {
            try {
                // Attempt to sign in with the intent result
                val user = googleAuthClient.signInWithIntent(result.data ?: return@launch)
                if (user.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    // Format error message using string resource with placeholder (%1$s)
                    val errorMsg = user.exceptionOrNull()?.message ?: ""
                    Toast.makeText(context, context.getString(R.string.error_msg_prefix, errorMsg), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                Toast.makeText(context, context.getString(R.string.error_msg_prefix, errorMsg), Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Local state for form inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Use BoxWithConstraints to make layout responsive to screen width
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        // Calculate padding based on available width (Responsive Design)
        val screenWidth = maxWidth
        val contentPadding = if (screenWidth < 600.dp) 20.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header Image ---
            Image(
                painter = painterResource(id = R.drawable.ic_images),
                contentDescription = stringResource(id = R.string.collage_desc),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- App Description ---
            Text(
                text = stringResource(id = R.string.lildescriptor),
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (screenWidth < 600.dp) 24.sp else 32.sp,
                lineHeight = 30.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Email Input ---
            CustomTextField(
                value = email,
                label = stringResource(id = R.string.mail_label),
                onValueChange = { email = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- Password Input ---
            CustomTextField(
                value = password,
                label = stringResource(id = R.string.password_label),
                onValueChange = { password = it },
                isPassword = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Login Button (Email/Password) ---
            AuthButton(
                text = stringResource(id = R.string.continue_button),
                loading = isLoading,
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(email.trim(), password.trim())
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onLoginSuccess()
                                } else {
                                    val errorMsg = task.exception?.message ?: ""
                                    Toast.makeText(context, context.getString(R.string.error_msg_prefix, errorMsg), Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, context.getString(R.string.empty_fields), Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // --- Google Sign-In Button ---
            AuthButton(
                text = stringResource(id = R.string.continue_with_google),
                icon = R.drawable.ic_google,
                backgroundColor = Color(0xFF757569),
                loading = false,
                onClick = {
                    launcher.launch(googleAuthClient.getSignInIntent())
                }
            )

            // --- Navigation Links ---
            ForgotPasswordText(onClick = onForgotPasswordClick)
            SignUpText(onClick = onNavigateToRegister)

            Spacer(modifier = Modifier.height(20.dp))
            Spacer(modifier = Modifier.height(30.dp))

            // --- Policies Text ---
            Text(
                text = stringResource(id = R.string.policies),
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A styled TextField component used for Authentication forms.
 */
@Composable
fun CustomTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontFamily = InterFont,
                fontSize = 16.sp,
                color = Color(0xFF868686)
            )
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .border(1.dp, Color.White, RoundedCornerShape(10.dp)),
        textStyle = TextStyle(color = Color.White, fontFamily = InterFont),
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

/**
 * A reusable Button component with loading state support and optional icon.
 */
@Composable
fun AuthButton(
    text: String,
    icon: Int? = null,
    backgroundColor: Color = Color(0xFF4D54BF),
    loading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Color.White)
        } else {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = InterFont
                    )
                    icon?.let {
                        Icon(
                            painter = painterResource(id = it),
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SignUpText(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "You don't have an account? "
        Text(
            text = stringResource(id = R.string.dont_account) + " ",
            style = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = InterFont
            )
        )

        // "Sign up" link
        Text(
            text = stringResource(id = R.string.sign_up),
            style = TextStyle(
                color = Color(0xFFF2E423),
                fontSize = 14.sp,
                fontFamily = InterFont,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.clickable { onClick() }
        )
    }
}

@Composable
fun ForgotPasswordText(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "Forgot your password?"
        Text(
            text = stringResource(id = R.string.forgot_password_question),
            style = TextStyle(
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = InterFont
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // "Reset password" link
        Text(
            text = stringResource(id = R.string.reset_password),
            style = TextStyle(
                color = Color(0xFFF2E423),
                fontSize = 14.sp,
                fontFamily = InterFont,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.clickable { onClick() }
        )
    }
}