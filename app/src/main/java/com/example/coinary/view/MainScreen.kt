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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val mainNavController = rememberNavController()

    //observar el backstack para obtener la ruta actual
    val navBackStackEntry by mainNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    //qué pantallas mostrar la barra inferior
    val showBottomBar = currentRoute in listOf(
        "home", "stats", "movement", "reminder", "notifications", "profile"
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = mainNavController)
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavigationGraph(navController = mainNavController)
        }
    }
}
