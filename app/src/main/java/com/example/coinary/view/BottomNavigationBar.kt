package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.coinary.R

@Composable
fun BottomNavigationBar(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp), // Aumenté un poco el espacio ya que hay menos botones
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 1. Botón Home (SE QUEDA IGUAL) ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { navController.navigate("home") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.home_icon),
                        contentDescription = "Home icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- (ELIMINADO) Botón de Estadísticas ---

            // --- 2. Botón de Añadir / Menú (SE QUEDA IGUAL) ---
            // Este botón abre el menú Grid
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { onMenuClick() }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.add_icon),
                        contentDescription = "Add icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // --- 3. Botón de Lápiz (MODIFICADO) ---
            // Ahora lleva a "movement" (Agregar Movimiento)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        // CAMBIO AQUÍ: Antes iba a "reminder", ahora va a "movement"
                        .clickable { navController.navigate("movement") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    // Mantenemos el icono del lápiz como pediste
                    Image(
                        painter = painterResource(id = R.drawable.pencil_icon),
                        contentDescription = "Movement icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}