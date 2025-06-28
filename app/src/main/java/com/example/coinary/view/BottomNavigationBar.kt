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
fun BottomNavigationBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Home
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
                        painter = painterResource(id = R.drawable.home_icon), // Pon el icono que tengas para Home
                        contentDescription = "Home icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Botón de Estadísticas (Stats)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { navController.navigate("stats") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.stats_icon),
                        contentDescription = "Stats icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Botón de Añadir (Add)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { navController.navigate("movement") }
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

            // Botón de Editar (Pencil)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clickable { navController.navigate("reminder") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.button_background),
                        contentDescription = "Button background",
                        modifier = Modifier.fillMaxSize()
                    )
                    Image(
                        painter = painterResource(id = R.drawable.pencil_icon),
                        contentDescription = "Pencil icon",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
