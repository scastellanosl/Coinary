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
// Ensure this class exists and is imported correctly
import com.example.coinary.view.PdfReportScreen

/**
 * MainScreen: The primary container for the authenticated application session.
 * It sets up the Scaffold structure, manages the Bottom Navigation Bar visibility based on
 * the current route, and hosts the navigation graph for all main application features.
 *
 * @param rootNavController The root navigation controller used for global navigation events
 * (e.g., logging out and returning to the Login screen).
 */
@Composable
fun MainScreen(rootNavController: NavHostController) {
    // 1. Internal controller for navigation within the Scaffold
    val mainNavController = rememberNavController()

    // 2. Detect current route to determine BottomBar visibility
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 3. Configuration of bottom bar visibility
    // Defines which screens should display the navigation bar
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
        "ant_expenses",
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
                        onAddNewClick = { mainNavController.navigate("add_menu") },
                        onLogout = {
                            // Navigate via root controller to clear session
                            rootNavController.navigate("login") {
                                popUpTo("main") { inclusive = true }
                            }
                        }
                    )
                }

                // --- MAIN MENU (GRID) ---
                composable("add_menu") {
                    AddMenuScreen(navController = mainNavController)
                }

                // --- ANT EXPENSES ---
                composable("ant_expenses") {
                    AntExpensesScreen(navController = mainNavController)
                }

                // --- DEBTS AND GOALS ---
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

                // --- MOVEMENTS ---
                composable("movement") {
                    MovementScreen(
                        navController = mainNavController,
                        onLogout = { }
                    )
                }

                // --- REMINDERS ---
                composable("reminder") {
                    ReminderScreen(navController = mainNavController)
                }

                // --- STATISTICS ---
                composable("stats") {
                    StatsScreen(navController = mainNavController)
                }

                // --- PROFILE ---
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

                // --- NOTIFICATIONS ---
                composable("notifications") {
                    NotificationsScreen(navController = mainNavController)
                }

                // --- RECOMMENDATIONS ---
                composable("recomendaciones") {
                    RecomendacionesPantalla(navController = mainNavController)
                }

                // ALTERNATIVE ROUTE (redundancy check for potential character encoding issues)
                composable("recomendaciones") {
                    RecomendacionesPantalla(navController = mainNavController)
                }

                // --- PREDICTIONS ---
                composable("predicciones") {
                    PrediccionesPantalla(navController = mainNavController)
                }

                // --- CURRENCY CONVERSION ---
                composable("currency_screen") {
                    CurrencyScreen(
                        navController = mainNavController,
                        onBackClick = { mainNavController.popBackStack() }
                    )
                }

                // --- PDF REPORTS ---
                composable("pdf_report") {
                    PdfReportScreen(navController = mainNavController)
                }
            }
        }
    }
}