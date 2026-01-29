package com.example.coinary.view

import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coinary.R
import com.example.coinary.view.InterFont
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && email.isNotBlank()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo más abajo
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.ic_image_register),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "Restablece tu contraseña",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(100.dp),
                textAlign = TextAlign.Center,
                fontFamily = InterFont
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
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
            textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = 16.sp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            isError = emailError != null
        )

        emailError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 40.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val trimmedEmail = email.trim()
                emailError = null

                if (!isValidEmail(trimmedEmail)) {
                    emailError = "Email inválido"
                    return@Button
                }

                isLoading = true  // ← usar isLoading
                auth.sendPasswordResetEmail(trimmedEmail)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Email enviado. Revisa bandeja/spam", Toast.LENGTH_LONG).show()
                            onBackToLogin()
                        } else {
                            // manejo errores...
                        }
                    }
            },
            enabled = !isLoading,  // ← usar isLoading
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.9f).height(45.dp).padding(horizontal = 20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Enviar email", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFamily = InterFont)
            }
        }


        Spacer(modifier = Modifier.height(24.dp))

        ClickableText(
            text = buildAnnotatedString { append("Volver al login") },
            modifier = Modifier.padding(4.dp),
            onClick = { onBackToLogin() },
            style = TextStyle(
                fontSize = 14.sp,
                color = Color(0xFF4D54BF),
                fontFamily = InterFont
            )
        )
    }
}


