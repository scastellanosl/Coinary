package com.example.coinary.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun MainScreen(rootNavController: NavHostController) {
    // 1. Controlador interno para la navegación dentro del Scaffold
    val mainNavController = rememberNavController()

    // 2. Detectar ruta actual para decidir si mostrar la BottomBar
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 3. Configuración de visibilidad de la barra inferior
    val showBottomBar = currentRoute in listOf(
        "home",
        "stats",
        "movement",
        "reminder",
        "notifications",
        "profile",
        "recomendaciones",
        "predicciones",
        "add_menu",
        "ant_expenses", // <--- Agregado para que se vea la barra en Gastos Hormiga
        "debts_goals/{activeTab}"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    navController = mainNavController,
                    onMenuClick = {
                        mainNavController.navigate("add_menu")
                    }
                )
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = mainNavController,
                startDestination = "home"
            ) {

                // --- HOME ---
                composable("home") {
                    HomeScreen(
                        navController = mainNavController,
                        onAddNewClick = {
                            mainNavController.navigate("add_menu")
                        },
                        onLogout = {
                            rootNavController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    )
                }

                // --- MENÚ PRINCIPAL (GRID) ---
                composable("add_menu") {
                    AddMenuScreen(navController = mainNavController)
                }

                // --- GASTOS HORMIGA (RUTA QUE FALTABA) ---
                composable("ant_expenses") {
                    AntExpensesScreen(navController = mainNavController)
                }

                // --- DEUDAS Y METAS ---
                composable(
                    route = "debts_goals/{activeTab}",
                    arguments = listOf(navArgument("activeTab") { type = NavType.StringType })
                ) { backStackEntry ->
                    val activeTab = backStackEntry.arguments?.getString("activeTab") ?: "deudas"
                    DebtsAndGoalsScreen(
                        navController = mainNavController,
                        initialTab = activeTab,
                        onBackClick = { mainNavController.popBackStack() }
                    )
                }

                // --- MOVIMIENTOS ---
                composable("movement") {
                    MovementScreen(
                        navController = mainNavController,
                        onLogout = { }
                    )
                }

                // --- RECORDATORIOS ---
                composable("reminder") {
                    ReminderScreen(navController = mainNavController)
                }

                // --- ESTADÍSTICAS ---
                composable("stats") {
                    StatsScreen(navController = mainNavController)
                }

                // --- PERFIL ---
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

                // --- NOTIFICACIONES ---
                composable("notifications") {
                    NotificationsScreen(navController = mainNavController)
                }

                // --- RECOMENDACIONES ---
                composable("recomendaciones") {
                    RecomendacionesPantalla(navController = mainNavController)
                }

                // --- PREDICCIONES ---
                composable("predicciones") {
                    PrediccionesPantalla(navController = mainNavController)
                }

                // --- DIVISA ---
                composable("currency_screen") {
                    CurrencyScreen(
                        navController = mainNavController,
                        onBackClick = { mainNavController.popBackStack() }
                    )
                }
            }
        }
    }
}