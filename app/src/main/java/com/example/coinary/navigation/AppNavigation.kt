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
// IMPORTAMOS LA VISTA CON EL NOMBRE CORRECTO
import com.example.coinary.view.MovementScreen
import com.example.coinary.view.*
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
                    Toast.makeText(context, "Error: ${signInResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
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
                    onLoginSuccess = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate("register") },
                    onForgotPasswordClick = { navController.navigate("reset_password") }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigate("main") { popUpTo("register") { inclusive = true } } },
                    onLoginClick = { navController.popBackStack() },
                    googleAuthClient = googleAuthClient,
                    launcher = launcher
                )
            }
            composable("reset_password") { ResetPasswordScreen(onBackToLogin = { navController.popBackStack() }) }

            // --- MAIN ---
            composable("main") { MainScreen(rootNavController = navController) }

            // --- HOME & MENU ---
            composable("home") {
                HomeScreen(
                    navController = navController,
                    onAddNewClick = { navController.navigate("add_menu") },
                    onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("login") { popUpTo("main") { inclusive = true } } }
                )
            }
            composable("add_menu") { AddMenuScreen(navController = navController) }

            // --- MOVIMIENTOS (AQUÍ ESTABA EL ERROR) ---
            // Actualizado para llamar a MovementScreen
            composable("movements_screen") {
                MovementScreen(navController = navController, onLogout = {})
            }
            composable("movement") {
                MovementScreen(navController = navController, onLogout = {})
            }

            // --- OTRAS PANTALLAS ---
            composable("reminder_screen") { ReminderScreen(navController = navController) }
            composable("reminder") { ReminderScreen(navController = navController) }
            composable("reports_screen") { StatsScreen(navController = navController) }
            composable("stats") { StatsScreen(navController = navController) }
            composable("currency_screen") { StatsScreen(navController = navController) }

            composable("ant_expenses") { AntExpensesScreen(navController = navController) }

            composable(
                route = "debts_goals/{activeTab}",
                arguments = listOf(navArgument("activeTab") { type = NavType.StringType })
            ) { backStackEntry ->
                val activeTab = backStackEntry.arguments?.getString("activeTab") ?: "deudas"
                DebtsAndGoalsScreen(navController = navController, initialTab = activeTab, onBackClick = { navController.popBackStack() })
            }

            composable("profile") {
                ProfileScreen(
                    navController = navController,
                    onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("login") { popUpTo("main") { inclusive = true } } }
                )
            }
            composable("notifications") {
                NotificationsScreen(
                    navController = navController,
                    onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("login") { popUpTo("main") { inclusive = true } } }
                )
            }
            composable("recommendations") { RecomendacionesPantalla(navController = navController) }
            composable("recomendaciones") { RecomendacionesPantalla(navController = navController) }
            composable("predictions") { PrediccionesPantalla(navController = navController) }
            composable("predicciones") { PrediccionesPantalla(navController = navController) }
        }
    }
}