package com.example.coinary.view

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape // Importar CircleShape
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
import androidx.compose.ui.draw.clip // Importar clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.min
import kotlin.random.Random

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
    val weeklyCategorizedExpenses by homeViewModel.weeklyCategorizedExpenses.collectAsState()

    // Formateador de moneda
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) } // Ejemplo para Colombia


    // --- NUEVO: Mapa de colores por categoría para consistencia ---
    val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
    val incomeCategories = context.resources.getStringArray(R.array.income_categories)

// Ejemplo: asigna colores a las categorías de gastos
    val categoryColorMap = remember {
        mapOf(
            expenseCategories[0] to Color(0xFFF2E423), // Food
            expenseCategories[1] to Color(0xFF4D54BF), // Transport
            expenseCategories[2] to Color(0xFFFFFFFF), // Housing
            expenseCategories[3] to Color(0xFFE91E63), // Entertainment
            expenseCategories[4] to Color(0xFFFF9800), // Services
            expenseCategories[5] to Color(0xFF9C27B0), // Shopping
            expenseCategories[6] to Color(0xFF00BCD4), // Health
            expenseCategories[7] to Color(0xFF8BC34A), // Education
            expenseCategories[8] to Color(0xFF03A9F4)  // Other Expenses
        ) + mapOf(
            incomeCategories[0] to Color(0xFF33CC33), // Salary
            incomeCategories[1] to Color(0xFF800080), // Gift
            incomeCategories[2] to Color(0xFFFFA500), // Sales
            incomeCategories[3] to Color(0xFF00CED1), // Investment
            incomeCategories[4] to Color(0xFFC0C0C0)  // Other Income
        )
    }


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
                    // Reemplazamos la imagen estática del gráfico por nuestro Composable de Canvas
                    // El fondo del contenedor de la gráfica se mantiene para el estilo
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

                        // Contenedor para la gráfica de pastel + texto centrado encima
                        Box(
                            modifier = Modifier
                                .width(screenWidth * 0.42f)
                                .height(screenHeight * 0.20f),
                            contentAlignment = Alignment.Center // Centra el contenido dentro de este Box
                        ) {
                            // Aquí se dibuja la gráfica de pastel dinámica
                            PieChartCanvas(
                                data = weeklyCategorizedExpenses,
                                totalAmount = weeklyCategorizedExpenses.values.sum(),
                                categoryColors = categoryColorMap // --- PASANDO EL MAPA DE COLORES ---
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
                                    text = currencyFormatter.format(weeklyCategorizedExpenses.values.sum()), // Ahora muestra el total de gastos de la semana
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

                // Mover la declaración de topExpenses aquí, fuera del Row, para que sea accesible
                val topExpenses = weeklyCategorizedExpenses
                    .entries
                    .sortedByDescending { it.value } // Ordenar de mayor a menor
                    .take(3) // Tomar las 3 primeras

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mapeo de categorías a recursos de imagen (usando fallbacks existentes)
                    val categoryIconMap = remember {
                        mapOf(
                            "Comida" to R.drawable.food_icon,
                            "Transporte" to R.drawable.car_icon,
                            "Vivienda" to R.drawable.home_icon,
                            "Ocio" to R.drawable.gift_icon, // Cambiado de "Entretenimiento" a "Ocio"
                            "Salario" to R.drawable.gift_icon, // Asegúrate de tener money_icon si lo usas
                            "Regalo" to R.drawable.gift_icon,
                            "Ventas" to R.drawable.gift_icon,
                            "Inversión" to R.drawable.gift_icon,
                            // Fallbacks para las categorías que no tienes iconos específicos
                            "Servicios" to R.drawable.home_icon, // Usando home_icon como fallback
                            "Compras" to R.drawable.food_icon, // Usando food_icon como fallback
                            "Salud" to R.drawable.gift_icon, // Usando gift_icon como fallback
                            "Educación" to R.drawable.car_icon, // Usando car_icon como fallback
                            "Otros Ingresos" to R.drawable.gift_icon, // Usando money_icon como fallback
                            "Otros Gastos" to R.drawable.gift_icon // Usando gift_icon como fallback genérico
                        )
                    }

                    // Colores de fondo para las tarjetas (puedes definir más en un array)
                    val cardBackgrounds = remember {
                        listOf(R.drawable.rectangle1, R.drawable.rectangle2, R.drawable.rectangle3)
                    }

                    topExpenses.forEachIndexed { index, (category, amount) ->
                        val bgRes = cardBackgrounds.getOrElse(index) { R.drawable.rectangle1 } // Cicla o usa un default
                        val iconRes = categoryIconMap[category] ?: R.drawable.gift_icon // Fallback si no hay icono específico
                        val categoryDisplayColor = categoryColorMap[category] ?: Color.Gray // Obtener el color para la tarjeta

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                painter = painterResource(id = bgRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                alpha = 0.9f
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // --- NUEVO: Indicador de color para la categoría ---
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp) // Tamaño pequeño para el círculo de color
                                            .clip(CircleShape) // Forma de círculo
                                            .background(categoryDisplayColor) // Color de la categoría
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = category, // Categoría dinámica
                                        color = Color.White,
                                        fontSize = 12.sp, // <-- REDUCIDO EL TAMAÑO DE LA FUENTE AQUÍ
                                        fontWeight = FontWeight.Bold
                                    )
                                }


                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(vertical = 6.dp)
                                )

                                Text(
                                    text = currencyFormatter.format(amount), // Cantidad dinámica
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                // Este bloque ahora tiene acceso a topExpenses
                if (topExpenses.size < 3) {
                    repeat(3 - topExpenses.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Composable para dibujar una gráfica de pastel dinámica.
 * @param data Un mapa de categoría a monto.
 * @param totalAmount El monto total para calcular porcentajes.
 * @param categoryColors Un mapa de categorías a colores específicos para el gráfico.
 */
@Composable
fun PieChartCanvas(data: Map<String, Double>, totalAmount: Double, categoryColors: Map<String, Color>) {
    // Si no hay datos o el total es 0, muestra un círculo gris para indicar que no hay gastos.
    if (data.isEmpty() || totalAmount == 0.0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height)
            val strokeWidth = diameter * 0.2f // Ancho del anillo
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            drawArc(
                color = Color.Gray.copy(alpha = 0.5f), // Color gris para indicar sin datos
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth)
            )
        }
        return
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = min(size.width, size.height)
        val strokeWidth = diameter * 0.2f // Ancho del anillo
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        val rect = Size(diameter, diameter)

        var startAngle = 0f
        // Ordena las secciones para que las más grandes vayan primero si lo deseas
        data.entries.sortedByDescending { it.value }.forEach { entry -> // Ya no necesitamos index aquí
            val sweepAngle = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
            // --- USANDO EL COLOR DEL MAPA categoryColors ---
            val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray } // Usa el color del mapa, o gris por defecto

            drawArc(
                color = sectionColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}
