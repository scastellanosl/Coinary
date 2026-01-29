package com.example.coinary.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home" // La pantalla inicial dentro de MainScreen
    ) {
        composable("home") {
            HomeScreen(
                navController = navController,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            GoogleLoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegister = {
                },
                onForgotPasswordClick = {
                    navController.navigate("reset_password")
                }
            )
        }


        composable("reset_password") {
            ResetPasswordScreen(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable("movement") {
            AddMovementScreen(navController = navController)
        }
        composable("reminder") {
            ReminderScreen(navController = navController)
        }
        composable("recomendaciones") {
            RecomendacionesPantalla(navController = navController)
        }
        composable("predicciones") {
            PrediccionesPantalla(navController = navController)
        }
        composable("notifications"){
            NotificationsScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                onLogout = { // Cierre de sesión
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
