package com.example.coinary.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
/**
 * HomeScreen: The central dashboard of the application.
 * Displays financial health indicators including total balance, a donut chart
 * of categorized expenses, and top spending categories.
 *
 * @param navController Navigation controller for screen transitions.
 * @param onAddNewClick Compatibility callback for adding new records (not currently used visually).
 * @param onLogout Triggers session termination and returns to login.
 * @param homeViewModel Business logic provider for financial calculations.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun HomeScreen(
    navController: NavController,
    onAddNewClick: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    // --- System UI Configuration ---
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color(0xFF150F33) // Dark color for the status bar

    // --- Context and Authentication ---
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val user = googleAuthClient.getSignedInUser()

    // --- ViewModel States ---
    val totalIncome by homeViewModel.totalIncome.collectAsState()
    val totalExpenses by homeViewModel.totalExpenses.collectAsState()
    val categorizedExpenses by homeViewModel.categorizedExpenses.collectAsState()
    val selectedTimeRange by homeViewModel.selectedTimeRange.collectAsState()

    // --- Currency Format ---
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // Load resource arrays
    val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
    val incomeCategories = context.resources.getStringArray(R.array.income_categories)

    // --- Category Translation Map ---
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

    // --- Color Map ---
    val categoryColorMap = remember(expenseCategories, incomeCategories) {
        mapOf(
            expenseCategories[0] to Color(0xFFF2E423), // Food
            expenseCategories[1] to Color(0xFF4D54BF), // Transport
            expenseCategories[2] to Color(0xFFFFFFFF), // Housing
            expenseCategories[3] to Color(0xFFE91E63), // Entertainment
            expenseCategories[4] to Color(0xFFFF9800), // Services
            expenseCategories[5] to Color(0xFF9C27B0), // Shopping
            expenseCategories[6] to Color(0xFF00BCD4), // Health
            expenseCategories[7] to Color(0xFF8BC34A), // Education
            expenseCategories[8] to Color(0xFF03A9F4)  // Other
        )
    }

    // --- Icon Map ---
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

    // --- Screen Configuration ---
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // --- User Preferences ---
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
        // REMOVED FLOATING ACTION BUTTON FROM HERE
        containerColor = Color.Black
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // --- 1. HEADER (Profile and Notifications) ---
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
                    // Avatar and Greeting
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(screenWidth * 0.02f)
                    ) {
                        if (photoUri.value != null) {
                            AsyncImage(
                                model = photoUri.value,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .size(screenHeight * 0.045f)
                                    .clip(CircleShape)
                                    .clickable { navController.navigate("profile") },
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.user_icon),
                                contentDescription = "Default Profile Photo",
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
                    // Action Icons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            "Notifications",
                            tint = Color(0xFFF2E423),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { navController.navigate("notifications") })
                        Icon(
                            Icons.Default.Lightbulb,
                            "Recommendations",
                            tint = Color(0xFFF2E423),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { navController.navigate("recomendaciones") })
                        Icon(
                            Icons.Default.TrendingUp,
                            "Predictions",
                            tint = Color(0xFFF2E423),
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { navController.navigate("predicciones") })
                    }
                }
            }

            // --- 2. BALANCE AND FILTER ---
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

                    // TOTAL EXPENSES (Large)
                    Text(
                        text = currencyFormatter.format(totalExpenses),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = screenHeight * 0.015f)
                    )

                    // INCOME (Small)
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

                    // --- Time Filter (Dropdown) ---
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
                                text = {
                                    Text(
                                        stringResource(R.string.filter_day),
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.DAY); expanded = false
                                })
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.filter_week),
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.WEEK); expanded = false
                                })
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.filter_month),
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.MONTH); expanded = false
                                })
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.filter_year),
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    homeViewModel.setTimeRange(TimeRange.YEAR); expanded = false
                                })
                        }
                    }
                }
            }

            // --- 3. CHART SECTION ---
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
                    Spacer(modifier = Modifier.weight(0.18f)) // Left space to center visually in the design
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

                            // Chart Canvas
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
                                // Chart Center Text (Donut)
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
                    Spacer(modifier = Modifier.weight(0.2f)) // Right space
                }
            }

            Spacer(modifier = Modifier.height(screenHeight * 0.01f))

            // --- 4. TOP EXPENSES LIST ---
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxHeight(0.68f)
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
                        .padding(
                            top = screenHeight * 0.008f,
                            start = screenWidth * 0.02f,
                            end = screenWidth * 0.02f,
                            bottom = screenHeight * 0.02f
                        )
                ) {
                    // Title very high up
                    Text(
                        text = stringResource(R.string.top_expenses),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(vertical = screenHeight * 0.003f),
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 5f
                            )
                        )
                    )

                    // HUGE SPACER to separate title from cards
                    Spacer(modifier = Modifier.height(screenHeight * 0.12f))

                    // Top 3 categories
                    val topExpenses =
                        categorizedExpenses.entries.sortedByDescending { it.value }.take(3)
                    val cardBackgrounds = listOf(
                        R.drawable.rectangle1,
                        R.drawable.rectangle2,
                        R.drawable.rectangle3
                    )

                    // Card Container - HORIZONTAL AND COMPACT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(horizontal = screenWidth * 0.01f),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = screenWidth * 0.018f,
                            alignment = Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.Top
                    ) {
                        for (i in 0 until 3) {
                            if (i < topExpenses.size) {
                                val entry = topExpenses[i]
                                val displayCategory = categoryTranslationMap[entry.key] ?: entry.key
                                val bgRes = cardBackgrounds.getOrElse(i) { R.drawable.rectangle1 }
                                val iconRes =
                                    categoryIconMap[displayCategory] ?: R.drawable.gift_icon
                                val categoryDisplayColor =
                                    categoryColorMap[displayCategory] ?: Color.Gray

                                // Card WIDER and LOWER (horizontal)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.1f), // WIDER THAN TALL (horizontal)
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Card Background
                                    Image(
                                        painter = painterResource(id = bgRes),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.FillBounds,
                                        alpha = 0.95f
                                    )

                                    // Card Content - COMPACT
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(
                                                vertical = screenHeight * 0.012f,
                                                horizontal = screenWidth * 0.012f
                                            )
                                    ) {
                                        // Category Label LARGER
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(screenWidth * 0.022f)
                                                    .clip(CircleShape)
                                                    .background(categoryDisplayColor)
                                            )
                                            Spacer(modifier = Modifier.width(screenWidth * 0.01f))
                                            Text(
                                                text = displayCategory,
                                                color = Color.White,
                                                fontSize = 12.sp, // LARGER
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    shadow = androidx.compose.ui.graphics.Shadow(
                                                        color = Color.Black.copy(alpha = 0.6f),
                                                        offset = androidx.compose.ui.geometry.Offset(
                                                            1f,
                                                            1f
                                                        ),
                                                        blurRadius = 3f
                                                    )
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(screenHeight * 0.005f))

                                        // Icon SMALLER
                                        Image(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(screenHeight * 0.038f) // SMALLER
                                        )

                                        Spacer(modifier = Modifier.height(screenHeight * 0.005f))

                                        // Price LARGER AND VISIBLE
                                        Text(
                                            text = currencyFormatter.format(entry.value),
                                            color = Color.White,
                                            fontSize = 12.sp, // LARGER
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = screenWidth * 0.005f),
                                            style = androidx.compose.ui.text.TextStyle(
                                                shadow = androidx.compose.ui.graphics.Shadow(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    offset = androidx.compose.ui.geometry.Offset(
                                                        1f,
                                                        1f
                                                    ),
                                                    blurRadius = 4f
                                                )
                                            )
                                        )
                                    }
                                }
                            } else {
                                // Horizontal empty card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.1f)
                                        .background(
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.gift_icon),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.12f),
                                        modifier = Modifier.size(screenHeight * 0.025f)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom space
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Custom Canvas component to draw the Donut type chart.
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
        var startAngle = -90f // Start from top

        // Sort descending for better visualization
        data.entries.sortedByDescending { it.value }.forEach { entry ->
            val sweepAngle = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
            val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray }

            drawArc(
                color = sectionColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false, // false = Donut (hollow), true = Pie (filled)
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweepAngle
        }
    }
}