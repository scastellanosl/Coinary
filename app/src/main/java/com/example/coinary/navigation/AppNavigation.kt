package com.example.coinary.navigation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.coinary.repository.GoogleAuthClient
import com.example.coinary.view.AddMenuScreen
import com.example.coinary.view.AntExpensesScreen
import com.example.coinary.view.DebtsAndGoalsScreen
import com.example.coinary.view.GoogleLoginScreen
import com.example.coinary.view.HomeScreen
import com.example.coinary.view.MainScreen
import com.example.coinary.view.MovementScreen
import com.example.coinary.view.NotificationsScreen
import com.example.coinary.view.PdfReportScreen
import com.example.coinary.view.PrediccionesPantalla
import com.example.coinary.view.ProfileScreen
import com.example.coinary.view.RecomendacionesPantalla
import com.example.coinary.view.RegisterScreen
import com.example.coinary.view.ReminderScreen
import com.example.coinary.view.ResetPasswordScreen
import com.example.coinary.view.StatsScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * AppNavigation: The central navigation hub for the Coinary application.
 * Defines the NavHost, routes, and transitions between different screens.
 * * Handles authentication state to determine the initial starting destination.
 */
@Composable
fun AppNavigation() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = rememberNavController()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    /**
     * Launcher for Google Sign-In activity results.
     * Manages the intent data and triggers the Firebase authentication flow.
     */
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            val intent = result.data
            if (intent != null) {
                val signInResult = googleAuthClient.signInWithIntent(intent)
                if (signInResult.isSuccess) {
                    // Navigate to Main upon successful Google Login
                    navController.navigate("main") {
                        popUpTo("register") { inclusive = true }
                    }
                } else {
                    // IMPORTANT ALERT: Notify the user about the authentication failure
                    val errorMsg = signInResult.exceptionOrNull()?.message ?: "Unknown Error"
                    Toast.makeText(context, "Authentication Failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Authentication state observer
    val isUserLoggedIn = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isUserLoggedIn.value = FirebaseAuth.getInstance().currentUser != null
    }

    // Wait for the auth state check to complete before rendering the NavHost
    isUserLoggedIn.value?.let { isLoggedIn ->
        val startDestination = if (isLoggedIn) "main" else "login"

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {

            // ====================================================================================
            // REGION: AUTHENTICATION FLOW
            // ====================================================================================

            composable("login") {
                GoogleLoginScreen(
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") },
                    onForgotPasswordClick = { navController.navigate("reset_password") }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("main") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onLoginClick = { navController.popBackStack() },
                    googleAuthClient = googleAuthClient,
                    launcher = launcher
                )
            }

            composable("reset_password") {
                ResetPasswordScreen(onBackToLogin = { navController.popBackStack() })
            }

            // ====================================================================================
            // REGION: MAIN APPLICATION SCREENS
            // ====================================================================================

            composable("main") { MainScreen(rootNavController = navController) }

            composable("home") {
                HomeScreen(
                    navController = navController,
                    onAddNewClick = { navController.navigate("add_menu") },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }
                )
            }

            composable("add_menu") { AddMenuScreen(navController = navController) }

            // ====================================================================================
            // REGION: FINANCIAL MANAGEMENT
            // ====================================================================================

            composable("movement") {
                MovementScreen(navController = navController, onLogout = {})
            }

            composable("ant_expenses") {
                AntExpensesScreen(navController = navController)
            }

            /**
             * Route: debts_goals
             * Param: activeTab (String) - Determines if 'Debts' or 'Goals' tab should open.
             */
            composable(
                route = "debts_goals/{activeTab}",
                arguments = listOf(navArgument("activeTab") { type = NavType.StringType })
            ) { backStackEntry ->
                val activeTab = backStackEntry.arguments?.getString("activeTab") ?: "deudas"
                DebtsAndGoalsScreen(
                    navController = navController,
                    initialTab = activeTab,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ====================================================================================
            // REGION: TOOLS & ANALYTICS
            // ====================================================================================

            composable("stats") { StatsScreen(navController = navController) }

            composable("reminder") { ReminderScreen(navController = navController) }

            composable("pdf_report") { PdfReportScreen(navController = navController) }

            // ====================================================================================
            // REGION: USER PROFILE & SETTINGS
            // ====================================================================================

            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }
                )
            }

            composable("notifications") {
                NotificationsScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") { popUpTo("main") { inclusive = true } }
                    }
                )
            }

            // ====================================================================================
            // REGION: SMART FEATURES (AI & RECOMMENDATIONS)
            // ====================================================================================

            composable("recommendations") { RecomendacionesPantalla(navController = navController) }

            composable("predictions") { PrediccionesPantalla(navController = navController) }

            // Legacy/Alias Routes for compatibility
            composable("movements_screen") { navController.navigate("movement") }
            composable("reminder_screen") { navController.navigate("reminder") }
            composable("reports_screen") { navController.navigate("stats") }
            composable("currency_screen") { navController.navigate("stats") }
        }
    }
}