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
 * NavigationGraph: Defines the root navigation structure of the application.
 * Determines the start destination based on the current Firebase Auth state
 * and maps all top-level screens, authentication flows, and feature modules.
 *
 * @param navController The NavHostController used to manage app navigation.
 */
@Composable
fun NavigationGraph(navController: NavHostController) {

    // --- Authentication State Check ---
    val auth = remember { FirebaseAuth.getInstance() }
    val isUserLoggedIn = auth.currentUser != null
    val startDestination = if (isUserLoggedIn) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- ROUTE: HOME DASHBOARD ---
        composable("home") {
            HomeScreen(
                navController = navController,
                // Navigates to the centralized "Add" menu (Grid layout)
                onAddNewClick = { navController.navigate("add_menu") },
                onLogout = {
                    // Clear backstack and return to login
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // --- ROUTE: LOGIN ---
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

        // --- ROUTE: REGISTER ---
        composable("register") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val googleAuthClient = remember { GoogleAuthClient(context) }

            // Launcher handling the Google Sign-In intent result for registration
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

        // --- ROUTE: RESET PASSWORD ---
        composable("reset_password") {
            ResetPasswordScreen(onBackToLogin = { navController.popBackStack() })
        }

        // --- ROUTE: ADD MENU (Central Action Hub) ---
        composable("add_menu") {
            AddMenuScreen(navController = navController)
        }

        // --- MENU DESTINATIONS ---

        // 1. Reminders (Mapped from 'reminder_screen' in AddMenu)
        composable("reminder_screen") {
            ReminderScreen(navController = navController)
        }

        // 2. Movements (Add Income/Expense)
        composable("movements_screen") {
            MovementScreen(navController = navController)
        }

        // 3. Stats (Reports)
        composable("reports_screen") {
            StatsScreen(navController = navController)
        }

        // 4. Recommendations
        composable("recomendaciones") {
            RecomendacionesPantalla(navController = navController)
        }

        // 5. Predictions
        composable("predicciones") {
            PrediccionesPantalla(navController = navController)
        }

        // 6. Notifications
        composable("notifications"){
            NotificationsScreen(navController = navController)
        }

        // 7. Profile
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