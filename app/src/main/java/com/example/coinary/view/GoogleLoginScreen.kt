@file:Suppress("DEPRECATION")

package com.example.coinary.view

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


val InterFont = FontFamily(Font(R.font.inter))

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GoogleLoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (googleAuthClient.getSignedInUser() != null) {
            onLoginSuccess()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        coroutineScope.launch {
            try {
                val user = googleAuthClient.signInWithIntent(result.data ?: return@launch)
                if (user.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "Error: ${user.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
    ) {
        val screenWidth = maxWidth
        val contentPadding = if (screenWidth < 600.dp) 20.dp else 40.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_images),
                contentDescription = "Collage Coinary",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = context.getString(R.string.lildescriptor),
                fontFamily = InterFont,
                fontWeight = FontWeight.Bold,
                fontSize = if (screenWidth < 600.dp) 24.sp else 32.sp,
                lineHeight = 30.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email
            CustomTextField(
                value = email,
                label = context.getString(R.string.mail_label),
                onValueChange = { email = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password
            CustomTextField(
                value = password,
                label = context.getString(R.string.password_label),
                onValueChange = { password = it },
                isPassword = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthButton(
                text = context.getString(R.string.continue_button),
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
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, context.getString(R.string.empty_fields), Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            AuthButton(
                text = context.getString(R.string.continue_with_google),
                icon = R.drawable.ic_google,
                backgroundColor = Color(0xFF757569),
                loading = false,
                onClick = {
                    launcher.launch(googleAuthClient.getSignInIntent())
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            SignUpText(onClick = onNavigateToRegister)

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = context.getString(R.string.policies),
                color = Color.White,
                fontSize = 9.sp,
                lineHeight = 12.sp,
                fontFamily = InterFont,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CustomTextField(value: String, label: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
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

@Composable
fun AuthButton(text: String, icon: Int? = null, backgroundColor: Color = Color(0xFF4D54BF), loading: Boolean, onClick: () -> Unit) {
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
    val context = LocalContext.current
    val annotatedText = buildAnnotatedString {
        append(context.getString(R.string.dont_account))
        pushStringAnnotation(tag = "SIGNUP", annotation = "signup")
        withStyle(
            style = SpanStyle(
                color = Color(0xFFF2E423),
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(context.getString(R.string.sign_up))
        }
        pop()
    }

    ClickableText(
        text = annotatedText,
        style = TextStyle(
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = InterFont,
            textAlign = TextAlign.Center
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { offset ->
            annotatedText.getStringAnnotations("SIGNUP", offset, offset).firstOrNull()?.let {
                onClick()
            }
        }
    )
}

