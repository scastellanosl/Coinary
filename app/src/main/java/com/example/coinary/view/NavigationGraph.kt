package com.example.coinary.view

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.coinary.repository.GoogleAuthClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun NavigationGraph(navController: NavHostController) {

    val auth = remember { FirebaseAuth.getInstance() }
    val isUserLoggedIn = auth.currentUser != null
    val startDestination = if (isUserLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Home Screen ---
        composable("home") {
            HomeScreen(
                navController = navController,
                // AQUÍ CONECTAMOS EL BOTÓN "+" DE LA HOME CON EL NUEVO MENÚ
                onAddNewClick = { navController.navigate("add_menu") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- Login Screen ---
        composable("login") {
            GoogleLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = { navController.navigate("register") },
                onForgotPasswordClick = { navController.navigate("reset_password") }
            )
        }

        // --- Register Screen ---
        composable("register") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val googleAuthClient = remember { GoogleAuthClient(context) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        if (account != null) {
                            scope.launch {
                                val signInResult = googleAuthClient.signInWithIntent(result.data ?: return@launch)
                                val user = signInResult.getOrNull()
                                if (user != null) {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    val error = signInResult.exceptionOrNull()?.message ?: "Unknown error"
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: ApiException) {
                        Toast.makeText(context, "Google sign in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                },
                onLoginClick = { navController.popBackStack() },
                googleAuthClient = googleAuthClient,
                launcher = launcher
            )
        }

        // --- Reset Password ---
        composable("reset_password") {
            ResetPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }

        // --- NUEVA PANTALLA DE MENÚ (+) ---
        composable("add_menu") {
            AddMenuScreen(navController = navController)
        }

        // --- DESTINOS DEL MENÚ ---

        // 1. Recordatorios (OJO: en AddMenuScreen pusimos 'reminder_screen', aquí lo mapeamos)
        composable("reminder_screen") {
            ReminderScreen(navController = navController)
        }

        // 2. Movimientos (Añadir Gasto/Ingreso)
        composable("movements_screen") {
            AddMovementScreen(navController = navController)
        }

        // 3. Stats (Reportes)
        composable("reports_screen") { // Antes era "stats"
            StatsScreen(navController = navController)
        }

        // 4. Recomendaciones
        composable("recomendaciones") {
            RecomendacionesPantalla(navController = navController)
        }

        // 5. Predicciones
        composable("predicciones") {
            PrediccionesPantalla(navController = navController)
        }

        // 6. Notificaciones
        composable("notifications"){
            NotificationsScreen(navController = navController)
        }

        // 7. Perfil
        composable("profile") {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}