package com.example.coinary.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.coinary.R
import com.example.coinary.repository.GoogleAuthClient
import com.example.coinary.viewmodel.HomeViewModel
import com.example.coinary.viewmodel.TimeRange
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

/**
 * Pantalla principal del dashboard.
 * Muestra el balance, gráfico de gastos y categorías principales.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomeScreen(
    navController: NavController,
    onAddNewClick: () -> Unit, // Callback mantenido por compatibilidad, aunque ya no se usa aquí visualmente
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    // --- Configuración de UI del Sistema ---
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color(0xFF150F33) // Color oscuro para la barra de estado

    // --- Contexto y Autenticación ---
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val user = googleAuthClient.getSignedInUser()

    // --- Estados del ViewModel ---
    val totalIncome by homeViewModel.totalIncome.collectAsState()
    val totalExpenses by homeViewModel.totalExpenses.collectAsState()
    val categorizedExpenses by homeViewModel.weeklyCategorizedExpenses.collectAsState()
    val selectedTimeRange by homeViewModel.selectedTimeRange.collectAsState()

    // --- Formato de Moneda ---
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // Cargar arrays de recursos
    val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
    val incomeCategories = context.resources.getStringArray(R.array.income_categories)

    // --- Mapa de Traducción de Categorías ---
    val categoryTranslationMap = remember(expenseCategories) {
        mapOf(
            "Comida" to expenseCategories[0], "Food" to expenseCategories[0],
            "Transporte" to expenseCategories[1], "Transport" to expenseCategories[1],
            "Vivienda" to expenseCategories[2], "Housing" to expenseCategories[2],
            "Ocio" to expenseCategories[3], "Entertainment" to expenseCategories[3],
            "Servicios" to expenseCategories[4], "Services" to expenseCategories[4],
            "Compras" to expenseCategories[5], "Shopping" to expenseCategories[5],
            "Salud" to expenseCategories[6], "Health" to expenseCategories[6],
            "Educación" to expenseCategories[7], "Education" to expenseCategories[7],
            "Otros" to expenseCategories[8], "Other" to expenseCategories[8]
        )
    }

    // --- Mapa de Colores ---
    val categoryColorMap = remember(expenseCategories, incomeCategories) {
        mapOf(
            expenseCategories[0] to Color(0xFFF2E423), // Comida
            expenseCategories[1] to Color(0xFF4D54BF), // Transporte
            expenseCategories[2] to Color(0xFFFFFFFF), // Vivienda
            expenseCategories[3] to Color(0xFFE91E63), // Ocio
            expenseCategories[4] to Color(0xFFFF9800), // Servicios
            expenseCategories[5] to Color(0xFF9C27B0), // Compras
            expenseCategories[6] to Color(0xFF00BCD4), // Salud
            expenseCategories[7] to Color(0xFF8BC34A), // Educación
            expenseCategories[8] to Color(0xFF03A9F4)  // Otros
        )
    }

    // --- Mapa de Iconos ---
    val categoryIconMap = remember(expenseCategories) {
        mapOf(
            expenseCategories[0] to R.drawable.food_icon,
            expenseCategories[1] to R.drawable.car_icon,
            expenseCategories[2] to R.drawable.home_icon,
            expenseCategories[3] to R.drawable.gift_icon,
            expenseCategories[4] to R.drawable.home_icon,
            expenseCategories[5] to R.drawable.food_icon,
            expenseCategories[6] to R.drawable.gift_icon,
            expenseCategories[7] to R.drawable.car_icon,
            expenseCategories[8] to R.drawable.gift_icon
        )
    }

    // --- Configuración de Pantalla ---
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // --- Preferencias de Usuario ---
    val prefs = remember { context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE) }
    val photoUri = remember { mutableStateOf(prefs.getString("photo_uri", null)?.toUri()) }
    val defaultUserName = stringResource(R.string.default_user_name)
    val displayName = remember {
        mutableStateOf(prefs.getString("display_name", null) ?: user?.username ?: defaultUserName)
    }

    SideEffect {
        systemUiController.setStatusBarColor(color = statusBarColor, darkIcons = false)
        val storedName = prefs.getString("display_name", null)
        val finalName = storedName ?: user?.username ?: defaultUserName
        if (displayName.value != finalName) displayName.value = finalName
        val storedPhoto = prefs.getString("photo_uri", null)?.toUri()
        if (photoUri.value != storedPhoto) photoUri.value = storedPhoto
    }

    Scaffold(
        // HE ELIMINADO EL FLOATING ACTION BUTTON DE AQUÍ
        containerColor = Color.Black
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // --- 1. CABECERA (Perfil y Notificaciones) ---
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.marco_superior),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = screenWidth * 0.02f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar y Saludo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(screenWidth * 0.02f)
                    ) {
                        if (photoUri.value != null) {
                            AsyncImage(
                                model = photoUri.value,
                                contentDescription = "Foto perfil",
                                modifier = Modifier
                                    .size(screenHeight * 0.045f)
                                    .clip(CircleShape)
                                    .clickable { navController.navigate("profile") },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.user_icon),
                                contentDescription = "Foto perfil defecto",
                                modifier = Modifier
                                    .size(screenHeight * 0.045f)
                                    .clip(CircleShape)
                                    .clickable { navController.navigate("profile") },
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.greeting),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF2E423),
                                fontSize = 12.sp
                            )
                            Text(
                                text = displayName.value,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF2E423),
                                fontSize = 16.sp
                            )
                        }
                    }
                    // Iconos de Acción
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            "Notificaciones",
                            tint = Color(0xFFF2E423),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { navController.navigate("notifications") })
                        Icon(
                            Icons.Default.Lightbulb,
                            "Recomendaciones",
                            tint = Color(0xFFF2E423),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { navController.navigate("recomendaciones") })
                        Icon(
                            Icons.Default.TrendingUp,
                            "Predicciones",
                            tint = Color(0xFFF2E423),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { navController.navigate("predicciones") })
                    }
                }
            }

            // --- 2. BALANCE Y FILTRO ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-screenHeight * 0.015f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.backgroundcoinary),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(0.95f),
                    contentScale = ContentScale.FillWidth
                )

                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.personal_expenses),
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color.White,
                        modifier = Modifier
                            .padding(top = screenHeight * 0.035f)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // GASTOS TOTALES (Grande)
                    Text(
                        text = currencyFormatter.format(totalExpenses),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = screenHeight * 0.015f)
                    )

                    // INGRESOS (Pequeño)
                    Text(
                        text = stringResource(R.string.total_income),
                        fontWeight = FontWeight.Thin,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = screenHeight * 0.005f)
                    )
                    Text(
                        text = currencyFormatter.format(totalIncome),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.offset(y = (-screenHeight * 0.005f))
                    )

                    // --- Filtro de Tiempo (Dropdown) ---
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.offset(y = (-screenHeight * 0.003f))) {
                        Button(
                            onClick = { expanded = true },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4D54BF),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(
                                horizontal = screenWidth * 0.06f,
                                vertical = screenHeight * 0.005f
                            )
                        ) {
                            val buttonText = when (selectedTimeRange) {
                                TimeRange.DAY -> stringResource(R.string.filter_day)
                                TimeRange.WEEK -> stringResource(R.string.filter_week)
                                TimeRange.MONTH -> stringResource(R.string.filter_month)
                                TimeRange.YEAR -> stringResource(R.string.filter_year)
                            }
                            Text(text = buttonText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF2B2B2B))
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_day), color = Color.White) },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.DAY); expanded = false
                                })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_week), color = Color.White) },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.WEEK); expanded = false
                                })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_month), color = Color.White) },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.MONTH); expanded = false
                                })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_year), color = Color.White) },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.YEAR); expanded = false
                                })
                        }
                    }
                }
            }

            // --- 3. GRÁFICO (Chart) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-screenHeight * 0.02f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.3f)
                ) {
                    Spacer(modifier = Modifier.weight(0.18f)) // Espacio izq para centrar visualmente en el diseño
                    Box(
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.fondo_contenedor_this_week),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .scale(1.03f),
                            contentScale = ContentScale.FillBounds
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val chartTitle = when (selectedTimeRange) {
                                TimeRange.DAY -> stringResource(R.string.title_this_day)
                                TimeRange.WEEK -> stringResource(R.string.title_this_week)
                                TimeRange.MONTH -> stringResource(R.string.title_this_month)
                                TimeRange.YEAR -> stringResource(R.string.title_this_year)
                            }

                            Text(
                                text = chartTitle,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(top = screenHeight * 0.015f)
                            )
                            Spacer(modifier = Modifier.height(screenHeight * 0.022f))

                            // Canvas del Gráfico
                            Box(
                                modifier = Modifier
                                    .width(screenWidth * 0.42f)
                                    .height(screenHeight * 0.20f),
                                contentAlignment = Alignment.Center
                            ) {
                                val translatedData = categorizedExpenses.mapKeys { (key, _) ->
                                    categoryTranslationMap[key] ?: key
                                }

                                PieChartCanvas(
                                    data = translatedData,
                                    totalAmount = translatedData.values.sum(),
                                    categoryColors = categoryColorMap
                                )
                                // Texto central del gráfico (Donut)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.all),
                                        fontWeight = FontWeight.Thin,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = currencyFormatter.format(translatedData.values.sum()),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(0.2f)) // Espacio der
                }
            }

            Spacer(modifier = Modifier.height(screenHeight * 0.01f))

            // --- 4. LISTA DE GASTOS TOP ---
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxHeight(0.68f) // Ajusta altura restante
            ) {
                Image(
                    painter = painterResource(id = R.drawable.marco_inferior),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(screenHeight * 0.02f)
                ) {
                    Text(
                        text = stringResource(R.string.top_expenses),
                        color = Color.White,
                        fontWeight = FontWeight.Thin,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.height(screenHeight * 0.05f))

                    // Top 3 categorías
                    val topExpenses =
                        categorizedExpenses.entries.sortedByDescending { it.value }.take(3)
                    val cardBackgrounds = listOf(
                        R.drawable.rectangle1,
                        R.drawable.rectangle2,
                        R.drawable.rectangle3
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = screenWidth * 0.02f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Iterar y mostrar tarjetas (o vacías si hay menos de 3)
                        for (i in 0 until 3) {
                            if (i < topExpenses.size) {
                                val entry = topExpenses[i]
                                val displayCategory =
                                    categoryTranslationMap[entry.key] ?: entry.key
                                val bgRes = cardBackgrounds.getOrElse(i) { R.drawable.rectangle1 }
                                val iconRes =
                                    categoryIconMap[displayCategory] ?: R.drawable.gift_icon
                                val categoryDisplayColor =
                                    categoryColorMap[displayCategory] ?: Color.Gray

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.85f)
                                        .padding(horizontal = screenWidth * 0.01f),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Image(
                                        painter = painterResource(id = bgRes),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds,
                                        alpha = 0.9f
                                    )
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = screenHeight * 0.015f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(screenWidth * 0.02f)
                                                    .clip(CircleShape)
                                                    .background(categoryDisplayColor)
                                            )
                                            Spacer(modifier = Modifier.width(screenWidth * 0.01f))
                                            Text(
                                                text = displayCategory,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        Image(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(screenHeight * 0.06f)
                                                .padding(vertical = screenHeight * 0.008f)
                                        )
                                        Text(
                                            text = currencyFormatter.format(entry.value),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                // Espacio vacío para mantener el diseño
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente Canvas personalizado para dibujar el gráfico de tipo Donut.
 */
@Composable
fun PieChartCanvas(
    data: Map<String, Double>,
    totalAmount: Double,
    categoryColors: Map<String, Color>
) {
    if (data.isEmpty() || totalAmount == 0.0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height)
            val strokeWidth = diameter * 0.2f
            val topLeft =
                Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = Color.Gray.copy(alpha = 0.5f),
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
        val strokeWidth = diameter * 0.2f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val rect = Size(diameter, diameter)
        var startAngle = -90f // Empezar desde arriba

        // Ordenar de mayor a menor para mejor visualización
        data.entries.sortedByDescending { it.value }.forEach { entry ->
            val sweepAngle = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
            val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray }

            drawArc(
                color = sectionColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false, // false = Donut (hueco), true = Pie (relleno)
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}