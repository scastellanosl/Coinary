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
    // 1. Controlador interno
    val mainNavController = rememberNavController()

    // 2. Detectar ruta actual
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 3. CAMBIO: Agregamos "add_menu" y "debts_goals" a la lista para que se vea la barra
    val showBottomBar = currentRoute in listOf(
        "home",
        "stats",
        "movement",
        "reminder",
        "notifications",
        "profile",
        "recomendaciones",
        "predicciones",
        "add_menu", // <--- AHORA EL MENÚ TIENE BARRA
        // Para que deudas también tenga barra (opcional, si quieres que se vea ahí también)
        "debts_goals/{activeTab}"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    navController = mainNavController,
                    // 4. CAMBIO: Ahora navegamos internamente, NO usamos rootNavController
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
                            // El botón flotante también navega interno ahora
                            mainNavController.navigate("add_menu")
                        },
                        onLogout = {
                            rootNavController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    )
                }

                // --- 5. CAMBIO: AGREGAMOS EL MENÚ AQUÍ ADENTRO ---
                composable("add_menu") {
                    AddMenuScreen(navController = mainNavController)
                }

                // --- 6. CAMBIO: TRAEMOS LA RUTA DE DEUDAS AQUÍ TAMBIÉN ---
                // (Para que los botones del menú funcionen dentro de MainScreen)
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
                    AddMovementScreen(
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