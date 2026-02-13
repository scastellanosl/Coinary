package com.example.coinary.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- 1. MODELO DE DATOS ---
data class MenuItem(
    val title: String,
    val route: String
)

// --- 2. CONFIGURACIÓN DEL MENÚ (ACTUALIZADA) ---
val menuOptions = listOf(
    // Enviamos un parámetro 'deudas'
    MenuItem("Deudas", "debts_goals/deudas"),

    // Usamos la ruta que ya configuramos en MainScreen
    MenuItem("Movimientos", "movement"),

    // Enviamos un parámetro 'ahorros' a la MISMA pantalla
    MenuItem("Ahorros", "debts_goals/ahorros"),

    // Asumo que Reportes es tu StatsScreen
    MenuItem("Reportes", "stats"),

    // Usamos la ruta que ya configuramos en MainScreen
    MenuItem("Recordatorios", "reminder"),

    MenuItem("Divisa", "currency_screen")
)

// --- 3. PANTALLA PRINCIPAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuScreen(
    navController: NavController
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { /* Sin título */ },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificaciones",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(menuOptions) { item ->
                        MenuGridItem(
                            item = item,
                            onClick = {
                                // Navegamos a la ruta definida en la lista
                                navController.navigate(item.route)
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- 4. COMPONENTE DE BOTÓN (Igual que antes) ---
@Composable
fun MenuGridItem(
    item: MenuItem,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2E2E4E),
            Color(0xFF1A1A2E)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush = gradientBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}