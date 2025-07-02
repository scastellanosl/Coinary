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
import androidx.compose.foundation.shape.CircleShape
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

@Composable
fun HomeScreen(
    navController: NavController,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
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
    val user = googleAuthClient.getSignedInUser()

    val totalIncome by homeViewModel.totalIncome.collectAsState()
    val totalExpenses by homeViewModel.totalExpenses.collectAsState()
    val weeklyCategorizedExpenses by homeViewModel.weeklyCategorizedExpenses.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }

    val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
    val incomeCategories = context.resources.getStringArray(R.array.income_categories)

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

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- Header Section ---
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.marco_superior),
                contentDescription = "Marco superior",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Adjust width relative to screen
                    .padding(horizontal = screenWidth * 0.02f), // Responsive padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(screenWidth * 0.02f) // Responsive spacing
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.user_icon),
                        contentDescription = "Foto de usuario",
                        modifier = Modifier
                            .size(screenHeight * 0.045f) // Responsive size
                            .clip(CircleShape) // Ensure it's circular
                            .clickable { navController.navigate("profile") }
                    )

                    Column {
                        Text(
                            context.getString(R.string.greeting),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF2E423),
                            fontSize = 12.sp // Keep relatively small
                        )
                        Text(
                            user?.username ?: "User",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF2E423),
                            fontSize = 16.sp // Readable size
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notification icon",
                    tint = Color(0xFFF2E423),
                    modifier = Modifier
                        .size(screenHeight * 0.04f) // Responsive size
                        .clickable { navController.navigate("notifications") }
                )
            }
        }

        // --- Balance Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-screenHeight * 0.015f)), // Slightly overlap with header
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.backgroundcoinary),
                contentDescription = "Background Coinary",
                modifier = Modifier.fillMaxWidth(0.95f), // Responsive width
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = context.getString(R.string.personal_expenses),
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp, // Fixed size, but could be responsive too
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = screenHeight * 0.035f) // Responsive top padding
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = currencyFormatter.format(totalExpenses),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = screenHeight * 0.015f) // Responsive padding
                )

                Text(
                    text = context.getString(R.string.total_income),
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

                Button(
                    onClick = { /* Change month action */ },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4D54BF),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.offset(y = (-screenHeight * 0.003f)),
                    contentPadding = PaddingValues(
                        horizontal = screenWidth * 0.06f,
                        vertical = screenHeight * 0.005f
                    ) // Responsive padding
                ) {
                    Text(
                        text = context.getString(R.string.month),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp // Keep readable
                    )
                }
            }
        }

        // --- Weekly Expenses Pie Chart Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-screenHeight * 0.02f)) // Adjusted overlap
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.3f) // Responsive height
            ) {
                // Left spacer
                Spacer(modifier = Modifier.weight(0.18f))

                // Main content container for chart
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

                        Box(
                            modifier = Modifier
                                .width(screenWidth * 0.42f) // Responsive width
                                .height(screenHeight * 0.20f), // Responsive height
                            contentAlignment = Alignment.Center
                        ) {
                            PieChartCanvas(
                                data = weeklyCategorizedExpenses,
                                totalAmount = weeklyCategorizedExpenses.values.sum(),
                                categoryColors = categoryColorMap
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
                                Text(
                                    text = currencyFormatter.format(weeklyCategorizedExpenses.values.sum()),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                // Right spacer
                Spacer(modifier = Modifier.weight(0.2f))
            }
        }

        Spacer(modifier = Modifier.height(screenHeight * 0.01f)) // Responsive spacer

        // --- Top Expenses Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .align(Alignment.CenterHorizontally)
                .fillMaxHeight(0.68f) // Responsive height
        ) {
            Image(
                painter = painterResource(id = R.drawable.marco_inferior),
                contentDescription = "Marco inferior",
                modifier = Modifier.fillMaxSize(), // Fill the box
                contentScale = ContentScale.FillBounds, // Scale to fill
                alignment = Alignment.Center
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(screenHeight * 0.02f) // Responsive padding
            ) {
                Text(
                    text = context.getString(R.string.top_expenses),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontWeight = FontWeight.Thin,
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(screenHeight * 0.05f)) // Responsive spacer

                val topExpenses = weeklyCategorizedExpenses
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)

                val categoryIconMap = remember {
                    mapOf(
                        "Comida" to R.drawable.food_icon,
                        "Transporte" to R.drawable.car_icon,
                        "Vivienda" to R.drawable.home_icon,
                        "Ocio" to R.drawable.gift_icon,
                        "Salario" to R.drawable.gift_icon,
                        "Regalo" to R.drawable.gift_icon,
                        "Ventas" to R.drawable.gift_icon,
                        "Inversión" to R.drawable.gift_icon,
                        "Servicios" to R.drawable.home_icon,
                        "Compras" to R.drawable.food_icon,
                        "Salud" to R.drawable.gift_icon,
                        "Educación" to R.drawable.car_icon,
                        "Otros Ingresos" to R.drawable.gift_icon,
                        "Otros Gastos" to R.drawable.gift_icon
                    )
                }

                val cardBackgrounds = remember {
                    listOf(R.drawable.rectangle1, R.drawable.rectangle2, R.drawable.rectangle3)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenWidth * 0.02f), // Responsive horizontal padding
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceAround // Use SpaceAround for even distribution
                ) {
                    topExpenses.forEachIndexed { index, (category, amount) ->
                        val bgRes = cardBackgrounds.getOrElse(index) { R.drawable.rectangle1 }
                        val iconRes = categoryIconMap[category] ?: R.drawable.gift_icon
                        val categoryDisplayColor = categoryColorMap[category] ?: Color.Gray

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f) // Maintain aspect ratio
                                .padding(horizontal = screenWidth * 0.01f), // Small horizontal padding between cards
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
                                    .padding(top = screenHeight * 0.015f) // Responsive top padding
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(screenWidth * 0.02f) // Responsive size
                                            .clip(CircleShape)
                                            .background(categoryDisplayColor)
                                    )
                                    Spacer(modifier = Modifier.width(screenWidth * 0.01f)) // Responsive width
                                    Text(
                                        text = category,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(screenHeight * 0.06f) // Responsive icon size
                                        .padding(vertical = screenHeight * 0.008f) // Responsive vertical padding
                                )

                                Text(
                                    text = currencyFormatter.format(amount),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    // If fewer than 3 expenses, add empty spaces to maintain layout
                    if (topExpenses.size < 3) {
                        repeat(3 - topExpenses.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(0.85f)) // Keep same size as other cards
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChartCanvas(data: Map<String, Double>, totalAmount: Double, categoryColors: Map<String, Color>) {
    if (data.isEmpty() || totalAmount == 0.0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height)
            val strokeWidth = diameter * 0.2f
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
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
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        val rect = Size(diameter, diameter)

        var startAngle = 0f
        data.entries.sortedByDescending { it.value }.forEach { entry ->
            val sweepAngle = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
            val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray }

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