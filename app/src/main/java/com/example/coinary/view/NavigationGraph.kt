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

/**
 * Defines the central navigation graph for the application.
 * Handles the routing between screens and determines the initial destination
 * based on the user's authentication state.
 *
 * @param navController The NavHostController used to manage app navigation.
 */
@Composable
fun NavigationGraph(navController: NavHostController) {

    // 1. Check current session state
    // We use a remembered instance of FirebaseAuth to check if a user is already signed in.
    val auth = remember { FirebaseAuth.getInstance() }
    val isUserLoggedIn = auth.currentUser != null

    // 2. Determine initial destination based on auth status
    // If logged in, skip to "home". Otherwise, start at "login".
    val startDestination = if (isUserLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- Home Screen ---
        composable("home") {
            HomeScreen(
                navController = navController,
                onLogout = {
                    // On logout, clear the entire back stack and navigate to login
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
                    // Navigate to home and remove login from back stack
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onForgotPasswordClick = {
                    navController.navigate("reset_password")
                }
            )
        }

        // --- Register Screen ---
        composable("register") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val googleAuthClient = remember { GoogleAuthClient(context) }

            // Launcher for Google Sign-In intent within the Register flow
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        if (account != null) {
                            scope.launch {
                                // Attempt to sign in with Firebase using the Google account
                                val signInResult = googleAuthClient.signInWithIntent(result.data ?: return@launch)

                                // Retrieve the user object (Result<T>)
                                val user = signInResult.getOrNull()

                                if (user != null) {
                                    // Success: Navigate to Home
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    // Failure: Show error message
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
                    // Navigate to home on successful email/password registration
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                googleAuthClient = googleAuthClient,
                launcher = launcher
            )
        }

        // --- Reset Password Screen ---
        composable("reset_password") {
            ResetPasswordScreen(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- Statistics Screen ---
        composable("stats") {
            StatsScreen(navController = navController)
        }

        // --- Add Movement (Income/Expense) Screen ---
        composable("movement") {
            AddMovementScreen(navController = navController)
        }

        // --- Reminders Screen ---
        composable("reminder") {
            ReminderScreen(navController = navController)
        }

        // --- Recommendations Screen ---
        composable("recomendaciones") {
            RecomendacionesPantalla(navController = navController)
        }

        // --- Predictions Screen ---
        composable("predicciones") {
            PrediccionesPantalla(navController = navController)
        }

        // --- Notifications Screen ---
        composable("notifications"){
            NotificationsScreen(navController = navController)
        }

        // --- Profile Screen ---
        composable("profile") {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    // Handle logout: Clear back stack (ID 0) and return to login
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}