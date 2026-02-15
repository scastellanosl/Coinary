package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.model.Expense
import com.example.coinary.viewmodel.MovementViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * AntExpensesScreen: Displays a detailed analysis of "Ant Expenses" (small, recurring leaks).
 * Includes monthly impact totals, annual projections, and AI-driven insights based
 * on spending patterns.
 */
@Composable
fun AntExpensesScreen(
    navController: NavController,
    viewModel: MovementViewModel = viewModel()
) {
    val systemUiController = rememberSystemUiController()
    val uiState by viewModel.uiState.collectAsState()

    // Filtered data stream specifically for expenses flagged as "Ant Expenses"
    val antExpenses = uiState.antExpenses

    // Aggregated statistics for visual feedback
    val totalAntMonth = antExpenses.sumOf { it.amount }
    val projectionYear = totalAntMonth * 12

    // Formatter instances prioritized for CO currency and localized dates
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    SideEffect {
        systemUiController.setStatusBarColor(color = Color.Black, darkIcons = false)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- BRANDING BACKGROUND ---
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 40.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // --- HEADER SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_desc),
                        tint = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.ant_expenses_title),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // IMPACT SUMMARY: Highlights the monthly loss and projected yearly impact
            ImpactCard(
                totalMonth = totalAntMonth,
                projectionYear = projectionYear,
                currencyFormat = currencyFormat
            )

            // DYNAMIC INSIGHT: Shows the top category leak using a localized template
            if (antExpenses.isNotEmpty()) {
                AntInsight(antExpenses = antExpenses)
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = stringResource(R.string.ant_auto_detection_label),
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = InterFont,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // --- LIST OF DETECTED LEAKS ---
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (antExpenses.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.ant_empty_state),
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                fontFamily = InterFont
                            )
                        }
                    }
                } else {
                    items(antExpenses) { expense ->
                        AntExpenseItem(expense, currencyFormat, dateFormat)
                    }
                }
            }
        }
    }
}

/**
 * ImpactCard: A high-contrast visual component to create awareness about financial leaks.
 */
@Composable
fun ImpactCard(totalMonth: Double, projectionYear: Double, currencyFormat: NumberFormat) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF150F33), Color(0xFF1B1A1A))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brush = gradientBrush)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(45.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.ant_impact_month_lost),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = InterFont
            )

            Text(
                text = currencyFormat.format(totalMonth).replace("COP", "$"),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ant_impact_projection) + " ",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = InterFont
                )
                Text(
                    text = currencyFormat.format(projectionYear).replace("COP", "$"),
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont
                )
            }
        }
    }
}

/**
 * AntInsight: Analyzes spend patterns to present actionable financial advice.
 */
@Composable
fun AntInsight(antExpenses: List<Expense>) {
    // Determine which category has the highest frequency of small spends
    val topCategory = antExpenses.groupBy { it.category }
        .maxByOrNull { it.value.size }?.key ?: "..."

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF150F33).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFD700))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                // Usage of templated stringResource to inject the category name dynamically
                text = stringResource(R.string.ant_insight_template, topCategory),
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = InterFont,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * AntExpenseItem: Stateless list item representing an individual detected ant expense.
 */
@Composable
fun AntExpenseItem(expense: Expense, currencyFormat: NumberFormat, dateFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = expense.description, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont, maxLines = 1)
            Text(text = expense.category, color = Color.Gray, fontSize = 12.sp, fontFamily = InterFont)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "-${currencyFormat.format(expense.amount).replace("COP", "$")}",
                color = Color(0xFFFF5252),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )
            Text(text = dateFormat.format(expense.date.toDate()), color = Color.Gray, fontSize = 11.sp, fontFamily = InterFont)
        }
    }
}