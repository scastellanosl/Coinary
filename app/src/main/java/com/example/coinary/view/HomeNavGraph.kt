package com.example.coinary.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * HomeNavGraph: Orchestrates the internal navigation flow of the main application.
 * This nested graph manages transitions between core features like Home, Movements, and Profile.
 * * @param mainNavController The controller responsible for navigating within the main app shell.
 * @param rootNavController The top-level controller used for global actions like logging out or opening the full-screen menu.
 */
@Composable
fun HomeNavGraph(
    mainNavController: NavHostController, // Internal tab navigation (Home, Profile, etc)
    rootNavController: NavHostController  // Global navigation for Login or Add Menu (+)
) {
    NavHost(
        navController = mainNavController,
        startDestination = "home"
    ) {
        /**
         * Route: home
         * Entry point of the authenticated application area.
         */
        composable("home") {
            HomeScreen(
                navController = mainNavController,
                onAddNewClick = {
                    // Navigate through the root controller to overlay the bottom bar with the Add Menu
                    rootNavController.navigate("add_menu")
                },
                onLogout = {
                    // Clear the backstack and return to the login flow
                    rootNavController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        /**
         * Route: movement
         * Handles the entry and editing of financial records.
         */
        composable("movement") {
            MovementScreen(
                navController = mainNavController,
                onLogout = { /* Optional session handling */ }
            )
        }

        /**
         * Route: stats
         * Displays financial analytics and charts.
         */
        composable("stats") {
            StatsScreen(navController = mainNavController)
        }

        /**
         * Route: profile
         * Manages user account settings and session termination.
         */
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

        /**
         * Route: notifications
         * Displays system and financial alerts.
         */
        composable("notifications") {
            NotificationsScreen(navController = mainNavController)
        }

        /**
         * Route: reminder
         * Interface for managing scheduled financial alerts.
         */
        composable("reminder") {
            ReminderScreen(navController = mainNavController)
        }

        /**
         * Route: recommendations (Recommendations)
         * Provides AI-driven financial advice.
         */
        composable("recommendations") {
            RecomendacionesPantalla(navController = mainNavController)
        }

        /**
         * Route: predictions (Predictions)
         * Forecasts future financial scenarios based on history.
         */
        composable("predictions") {
            PrediccionesPantalla(navController = mainNavController)
        }
    }
}