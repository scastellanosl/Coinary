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
import com.example.coinary.view.AddMovementScreen
import com.example.coinary.view.DebtsAndGoalsScreen // <--- 1. IMPORTANTE: Importa el nombre correcto
import com.example.coinary.view.GoogleLoginScreen
import com.example.coinary.view.HomeScreen
import com.example.coinary.view.MainScreen
import com.example.coinary.view.NotificationsScreen
import com.example.coinary.view.PrediccionesPantalla
import com.example.coinary.view.ProfileScreen
import com.example.coinary.view.RecomendacionesPantalla
import com.example.coinary.view.RegisterScreen
import com.example.coinary.view.ReminderScreen
import com.example.coinary.view.ResetPasswordScreen
import com.example.coinary.view.StatsScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = rememberNavController()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            val intent = result.data
            if (intent != null) {
                val signInResult = googleAuthClient.signInWithIntent(intent)
                if (signInResult.isSuccess) {
                    navController.navigate("main") {
                        popUpTo("register") { inclusive = true }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Error: ${signInResult.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val isUserLoggedIn = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isUserLoggedIn.value = FirebaseAuth.getInstance().currentUser != null
    }

    isUserLoggedIn.value?.let { isLoggedIn ->
        val startDestination = if (isLoggedIn) "main" else "login"

        NavHost(navController = navController, startDestination = startDestination) {

            // --- LOGIN & REGISTER ---
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

            // --- MAIN & HOME ---
            composable("main") {
                MainScreen(rootNavController = navController)
            }

            composable("home") {
                HomeScreen(
                    navController = navController,
                    onAddNewClick = { navController.navigate("add_menu") },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

            // --- PANTALLA DEL MENÚ (GRID) ---
            composable("add_menu") {
                AddMenuScreen(navController = navController)
            }

            // --- DESTINOS DEL MENÚ ---
            composable("reminder_screen") { ReminderScreen(navController = navController) }
            composable("reminder") { ReminderScreen(navController = navController) }
            composable("movements_screen") { AddMovementScreen(navController = navController, onLogout = {}) }
            composable("movement") { AddMovementScreen(navController = navController, onLogout = {}) }
            composable("reports_screen") { StatsScreen(navController = navController) }
            composable("stats") { StatsScreen(navController = navController) }
            composable("currency_screen") { StatsScreen(navController = navController) }

            // 5. DEUDAS Y METAS (CORREGIDO)
            composable(
                route = "debts_goals/{activeTab}",
                arguments = listOf(navArgument("activeTab") { type = NavType.StringType })
            ) { backStackEntry ->
                val activeTab = backStackEntry.arguments?.getString("activeTab") ?: "deudas"

                // 2. CORREGIDO: Nombre de la función y nombre del parámetro
                DebtsAndGoalsScreen(
                    navController = navController,
                    initialTab = activeTab,
                    onBackClick = { navController.popBackStack() } // <--- onBackClick, no onBack
                )
            }

            // --- OTRAS PANTALLAS ---
            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

            composable("notifications") {
                NotificationsScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

            composable("recommendations") { RecomendacionesPantalla(navController = navController) }
            composable("recomendaciones") { RecomendacionesPantalla(navController = navController) }
            composable("predictions") { PrediccionesPantalla(navController = navController) }
            composable("predicciones") { PrediccionesPantalla(navController = navController) }
        }
    }
}