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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.coinary.repository.GoogleAuthClient
import com.example.coinary.view.AddMovementScreen
import com.example.coinary.view.GoogleLoginScreen
import com.example.coinary.view.HomeScreen
import com.example.coinary.view.MainScreen
import com.example.coinary.view.NotificationsScreen
import com.example.coinary.view.PredictionScreen
import com.example.coinary.view.ProfileScreen
import com.example.coinary.view.RecommendationsScreen
import com.example.coinary.view.RegisterScreen
import com.example.coinary.view.UserSetupScreen
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

    // Verificar si el usuario está logeado (FirebaseAuth)
    val isUserLoggedIn = remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        isUserLoggedIn.value = FirebaseAuth.getInstance().currentUser != null
    }

    // ✅ Mientras se verifica el estado de autenticación, no renderizamos nada
    isUserLoggedIn.value?.let { isLoggedIn ->
        val startDestination = if (isLoggedIn) "main" else "login"

        NavHost(navController = navController, startDestination = startDestination) {

            composable("login") {
                GoogleLoginScreen(
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }

                composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate("user_setup") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        navController.popBackStack()
                    },
                    googleAuthClient = googleAuthClient,
                    launcher = launcher
                )
            }

            composable("main") {
                MainScreen(rootNavController = navController)
            }
            composable("home") {
                HomeScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

            composable("movement") {
                AddMovementScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

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

            composable("user_setup") {
                UserSetupScreen(
                    onSetupComplete = {
                        navController.navigate("main") {
                            popUpTo("user_setup") { inclusive = true }
                        }
                    }
                )
            }

            composable("recomendations") {
                RecommendationsScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

            composable("predictions") {
                PredictionScreen(
                    navController = navController,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }

        }
    }
}
