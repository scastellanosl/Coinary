package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.example.coinary.viewmodel.HomeViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel() // Inyecta el HomeViewModel
) {

    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color(0xFF150F33)
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val user = googleAuthClient.getSignedInUser()

    // Observar los totales desde el HomeViewModel
    val totalIncome by homeViewModel.totalIncome.collectAsState()
    val totalExpenses by homeViewModel.totalExpenses.collectAsState()

    // Formateador de moneda
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) } // Ejemplo para Colombia


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.marco_superior),
                contentDescription = "Marco superior",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(0.95f),
                horizontalArrangement = Arrangement.Center
            ) {

                Row(
                    horizontalArrangement = Arrangement.Start
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.user_icon),
                        contentDescription = "Foto de usuario",
                        modifier = Modifier
                            .fillMaxWidth(0.1f)
                            .size(35.dp)
                            .clickable {
                                navController.navigate("profile")
                            }
                    )
                }


                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        context.getString(R.string.greeting),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF2E423),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        user?.username ?: "User",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF2E423)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification icon",
                        tint = Color(0xFFF2E423),
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                navController.navigate("notifications")
                            }
                            .padding(top = 3.dp)
                    )

                }

            }

        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Box(modifier = Modifier.fillMaxWidth(0.95f)) {
                Image(
                    painter = painterResource(id = R.drawable.backgroundcoinary),
                    contentDescription = "Background Coinary",
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }

            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.personal_expenses),
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Mostrar el total de gastos
                Text(
                    text = currencyFormatter.format(totalExpenses), // Usar el total de gastos real
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 15.dp)
                )

                Text(
                    text = context.getString(R.string.total_income),
                    fontWeight = FontWeight.Thin,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 3.dp)
                )

                // Mostrar el total de ingresos
                Text(
                    text = currencyFormatter.format(totalIncome), // Usar el total de ingresos real
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.offset(y = (-5).dp)
                )

                Button(
                    onClick = { /* Change month action */ },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4D54BF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.offset(y = (-3).dp),
                    contentPadding = PaddingValues(horizontal = 25.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = context.getString(R.string.month),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

        }

        val screenHeight =
            LocalConfiguration.current.screenHeightDp.dp //Para aplicar responsividad, conociendo las dimensiones del dispositivo
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-16).dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.3f)
            ) {
                // Espacio izquierdo
                Box(modifier = Modifier.weight(0.18f))

                // Contenedor principal
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.fondo_contenedor_this_week),
                        contentDescription = "Fondo contenedor this week",
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.03f),
                        contentScale = ContentScale.FillBounds
                    )


                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = context.getString(R.string.this_week),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(top = screenHeight * 0.015f)
                        )

                        Spacer(modifier = Modifier.height(screenHeight * 0.022f))

                        // Contenedor para la imagen de pastel + texto centrado encima
                        Box(
                            modifier = Modifier
                                .width(screenWidth * 0.42f)
                                .height(screenHeight * 0.20f)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.cake_graphic),
                                contentDescription = "Cake graphic",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = context.getString(R.string.all),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Thin,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                // Aquí podrías mostrar el "balance" o el total de la semana si lo calculas
                                Text(
                                    text = currencyFormatter.format(totalIncome - totalExpenses), // Ejemplo: balance total
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                // Porcentaje restante para rellenar la screen
                Box(modifier = Modifier.weight(0.2f))
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .align(Alignment.CenterHorizontally)
                .fillMaxHeight(0.68f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.marco_inferior),
                contentDescription = "Marco inferior",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = context.getString(R.string.top_expenses),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Thin,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(50.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nota: Estos datos de gastos principales siguen siendo estáticos.
                    // Para hacerlos dinámicos, necesitarías una lógica más compleja en el ViewModel
                    // para agrupar y sumar gastos por categoría.
                    listOf(
                        Triple(context.getString(R.string.food), R.drawable.rectangle1, R.drawable.food_icon) to "$ 102.500",
                        Triple(context.getString(R.string.gifts), R.drawable.rectangle2, R.drawable.gift_icon) to "$ 78.000",
                        Triple(context.getString(R.string.transport), R.drawable.rectangle3, R.drawable.car_icon) to "$ 65.123",
                    ).forEach { (triple, price) ->
                        val (label, bgRes, iconRes) = triple
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f) // para que no sean cuadrados perfectos pero sí proporcionados
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                painter = painterResource(id = bgRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                alpha = 0.9f // para no tapar el texto completamente
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(vertical = 6.dp)
                                )

                                Text(
                                    text = price,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
