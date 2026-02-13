package com.example.coinary.view

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coinary.R
import com.google.accompanist.systemuicontroller.rememberSystemUiController

// --- 1. MODELO DE DATOS ---
data class MenuItem(
    val title: String,
    val route: String
)

// --- 2. CONFIGURACIÓN DEL MENÚ ---
val menuOptions = listOf(
    MenuItem("Deudas", "debts_goals/deudas"),
    MenuItem("Movimientos", "movement"),
    MenuItem("Ahorros", "debts_goals/ahorros"),
    MenuItem("Reportes", "stats"),
    MenuItem("Recordatorios", "reminder"),
    MenuItem("Gastos Hormiga", "ant_expenses"),
    MenuItem("Divisa", "currency_screen")

)

// --- 3. PANTALLA PRINCIPAL ---
@Composable
fun AddMenuScreen(
    navController: NavController
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Black,
            darkIcons = false
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- IMAGEN DE FONDO ---
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- BARRA SUPERIOR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White
                    )
                }
            }

            // Espacio para bajar los botones
            Spacer(modifier = Modifier.height(150.dp))

            // --- GRID DE OPCIONES ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(menuOptions) { item ->
                    MenuGridItem(
                        item = item,
                        onClick = {
                            navController.navigate(item.route)
                        }
                    )
                }
            }
        }
    }
}

// --- 4. COMPONENTE DE BOTÓN (CON EL NUEVO GRADIENTE) ---
@Composable
fun MenuGridItem(
    item: MenuItem,
    onClick: () -> Unit
) {
    // MODIFICADO: Se actualizaron los colores del gradiente según tu solicitud.
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF150F33), // Color inicial solicitado
            Color(0xFF282626)  // Color final solicitado
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp)) // Bordes redondeados como en la referencia
            .background(brush = gradientBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            // fontFamily = InterFont // Descomenta si usas tu fuente personalizada
        )
    }
}