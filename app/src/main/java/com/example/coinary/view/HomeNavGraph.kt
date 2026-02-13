package com.example.coinary.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun HomeNavGraph(
    mainNavController: NavHostController, // Para navegar entre pestañas (Home, Profile, etc)
    rootNavController: NavHostController  // Para salir al Login o ir al Menú (+)
) {
    NavHost(
        navController = mainNavController,
        startDestination = "home"
    ) {
        // --- 1. HOME ---
        composable("home") {
            HomeScreen(
                navController = mainNavController,
                onAddNewClick = {
                    // Usamos root para que el menú tape la barra inferior (opcional)
                    // O mainNavController si quieres que se mantenga la barra
                    rootNavController.navigate("add_menu")
                },
                onLogout = {
                    // Usamos root para volver al login y matar la sesión
                    rootNavController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        // --- 2. MOVEMENT (¡ESTO FALTABA Y CAUSABA EL ERROR!) ---
        composable("movement") {
            AddMovementScreen(
                navController = mainNavController,
                onLogout = { /* Lógica opcional */ }
            )
        }

        // --- 3. OTRAS PESTAÑAS ---
        composable("stats") {
            StatsScreen(navController = mainNavController)
        }

        composable("profile") {
            ProfileScreen(
                navController = mainNavController,
                onLogout = {
                    rootNavController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable("notifications") {
            NotificationsScreen(navController = mainNavController)
        }

        composable("reminder") { // O "reminder_screen" según tu menú
            ReminderScreen(navController = mainNavController)
        }

        composable("recomendaciones") {
            RecomendacionesPantalla(navController = mainNavController)
        }

        composable("predicciones") {
            PrediccionesPantalla(navController = mainNavController)
        }
    }
}