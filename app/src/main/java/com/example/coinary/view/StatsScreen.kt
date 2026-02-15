package com.example.coinary.view

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.DatePicker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.viewmodel.ChartType
import com.example.coinary.viewmodel.MonthlySummary
import com.example.coinary.viewmodel.StatsViewModel
import com.example.coinary.viewmodel.TransactionFilter
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

// --- MODERN COLOR PALETTE ---
private val AppBackground = Color(0xFF0F0F1A) // Deep Midnight Blue
private val SurfaceColor = Color(0xFF1E1E2E)   // Card Surface
private val PrimaryColor = Color(0xFF5E5ADB)   // Modern Indigo
private val OnPrimaryColor = Color.White
private val IncomeColor = Color(0xFF00C853)    // Vibrant Green
private val ExpenseColor = Color(0xFFFF4757)   // Modern Red
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0C0)  // Blue-ish Gray

/**
 * StatsScreen: Displays financial statistics using charts and lists.
 * Allows filtering by date (month/year), transaction type, and chart representation (Bar/Pie).
 *
 * @param navController Navigation controller for screen transitions.
 * @param statsViewModel ViewModel managing financial data aggregation.
 */
@SuppressLint("LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel = viewModel()
) {
    // --- System UI Setup ---
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = AppBackground,
            darkIcons = false
        )
    }

    val context = LocalContext.current

    // --- State Observation ---
    val selectedMonth by statsViewModel.selectedMonth.collectAsState()
    val selectedYear by statsViewModel.selectedYear.collectAsState()
    val selectedChartType by statsViewModel.selectedChartType.collectAsState()
    val selectedTransactionFilter by statsViewModel.selectedTransactionFilter.collectAsState()

    val monthlyIncomes by statsViewModel.monthlyIncomes.collectAsState()
    val monthlyExpenses by statsViewModel.monthlyExpenses.collectAsState()
    val filteredTransactions by statsViewModel.filteredTransactions.collectAsState()
    val monthlySummaries by statsViewModel.monthlySummaries.collectAsState()

    // State for interactive chart selection (Bar Chart drill-down)
    var selectedBarTransactions by remember { mutableStateOf<List<Any>?>(null) }
    var selectedBarLabel by remember { mutableStateOf("") }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // --- Resources ---
    val monthNames = context.resources.getStringArray(R.array.months).toList()
    val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
    val incomeCategories = context.resources.getStringArray(R.array.income_categories)

    // Maps database keys (often English or mixed) to localized display names
    val categoryNameMap = remember(expenseCategories, incomeCategories) {
        mapOf(
            "Comida" to expenseCategories[0], "Food" to expenseCategories[0],
            "Transporte" to expenseCategories[1], "Transport" to expenseCategories[1],
            "Vivienda" to expenseCategories[2], "Housing" to expenseCategories[2],
            "Ocio" to expenseCategories[3], "Entertainment" to expenseCategories[3],
            "Servicios" to expenseCategories[4], "Services" to expenseCategories[4],
            "Compras" to expenseCategories[5], "Shopping" to expenseCategories[5],
            "Salud" to expenseCategories[6], "Health" to expenseCategories[6],
            "Educación" to expenseCategories[7], "Education" to expenseCategories[7],
            "Otros Gastos" to expenseCategories[8], "Other Expenses" to expenseCategories[8],
            "Pago Deuda" to "Debt Payment",
            "Ahorro" to "Savings",
            "Salario" to incomeCategories[0], "Salary" to incomeCategories[0],
            "Regalo" to incomeCategories[1], "Gift" to incomeCategories[1],
            "Ventas" to incomeCategories[2], "Sales" to incomeCategories[2],
            "Inversión" to incomeCategories[3], "Investment" to incomeCategories[3],
            "Otros Ingresos" to incomeCategories[4], "Other Income" to incomeCategories[4]
        )
    }

    // Maps categories to specific colors for charts
    val categoryColorMap = remember(expenseCategories, incomeCategories) {
        mapOf(
            expenseCategories[0] to Color(0xFFFFC107), // Food
            expenseCategories[1] to Color(0xFF5C6BC0), // Transport
            expenseCategories[2] to Color(0xFF8D6E63), // Housing
            expenseCategories[3] to ExpenseColor,      // Entertainment
            expenseCategories[4] to Color(0xFFFF9800), // Services
            expenseCategories[5] to Color(0xFFAB47BC), // Shopping
            expenseCategories[6] to Color(0xFF26C6DA), // Health
            expenseCategories[7] to Color(0xFF9CCC65), // Education
            expenseCategories[8] to Color(0xFF78909C), // Other
            "Pago Deuda" to Color(0xFFFF5722),
            "Debt Payment" to Color(0xFFFF5722),
            "Ahorro" to Color(0xFF2E7D32),
            "Savings" to Color(0xFF2E7D32),
            incomeCategories[0] to IncomeColor,        // Salary
            incomeCategories[1] to Color(0xFFBA68C8),  // Gift
            incomeCategories[2] to Color(0xFFFFB74D),  // Sales
            incomeCategories[3] to Color(0xFF4DD0E1),  // Investment
            incomeCategories[4] to Color(0xFFBDBDBD)   // Other Income
        )
    }

    Scaffold(
        containerColor = AppBackground
    ) { paddingValues ->

        // Dialog showing transactions when a specific bar in the chart is clicked
        selectedBarTransactions?.let { transactions ->
            TransactionListDialog(
                transactions = transactions,
                label = selectedBarLabel,
                categoryNameMap = categoryNameMap,
                currencyFormatter = currencyFormatter,
                onDismiss = { selectedBarTransactions = null }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Header ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.background(SurfaceColor, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.stats_title), // "Statistics"
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- Month/Year Selector ---
            item {
                MonthYearPickerModern(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onDateSelected = { month, year -> statsViewModel.updateSelectedDate(month, year) },
                    monthNames = monthNames
                )
            }

            // --- Chart Type Selector (Bar vs Pie) ---
            item {
                ChartTypeSelectorModern(selectedChartType = selectedChartType) { type ->
                    statsViewModel.updateChartType(type)
                }
            }

            // --- Main Chart Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val transactionsForDetail = when (selectedTransactionFilter) {
                            TransactionFilter.INCOME -> monthlyIncomes
                            TransactionFilter.EXPENSE -> monthlyExpenses
                            TransactionFilter.ALL -> monthlyIncomes + monthlyExpenses
                        }

                        // Chart Rendering
                        when (selectedChartType) {
                            ChartType.BAR -> {
                                MonthlyBarChartImproved(
                                    data = monthlySummaries,
                                    incomeColor = IncomeColor,
                                    expenseColor = ExpenseColor,
                                    currencyFormatter = currencyFormatter,
                                    monthNames = monthNames,
                                    incomeLabel = stringResource(R.string.legend_income), // "Income"
                                    expenseLabel = stringResource(R.string.legend_expense), // "Expense"
                                    onBarClick = { label, month, year, isIncome ->
                                        // Filter transactions for the specific clicked month
                                        val transactionsToShow = if (isIncome) {
                                            monthlyIncomes.filter {
                                                val date = it.date.toDate()
                                                val cal = Calendar.getInstance().apply { time = date }
                                                cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.YEAR) == year
                                            }
                                        } else {
                                            monthlyExpenses.filter {
                                                val date = it.date.toDate()
                                                val cal = Calendar.getInstance().apply { time = date }
                                                cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.YEAR) == year
                                            }
                                        }
                                        selectedBarLabel = label
                                        selectedBarTransactions = transactionsToShow
                                    }
                                )
                            }
                            else -> {
                                val rawData = statsViewModel.getCategorizedTotalsForPieChart(transactionsForDetail as List<Any>)
                                val translatedData = rawData.mapKeys { (key, _) -> categoryNameMap[key] ?: key }
                                ImprovedPieChartModern(
                                    data = translatedData,
                                    categoryColors = categoryColorMap,
                                    currencyFormatter = currencyFormatter
                                )
                            }
                        }

                        // Empty State within Card
                        val noData = when (selectedChartType) {
                            ChartType.BAR -> monthlySummaries.isEmpty()
                            else -> transactionsForDetail.isEmpty()
                        }
                        if (noData) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.no_data_selected_month),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // --- Transaction Filter (All/Income/Expense) ---
            item {
                TransactionFilterSelectorModern(selectedFilter = selectedTransactionFilter) { filter ->
                    statsViewModel.updateTransactionFilter(filter)
                }
            }

            // --- Transaction List ---
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_data_selected_month),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    val originalCategory = if (transaction is Income) transaction.category else (transaction as Expense).category
                    val translatedCategory = categoryNameMap[originalCategory] ?: originalCategory

                    TransactionCardSmall(
                        transaction = transaction,
                        categoryNameMap = categoryNameMap,
                        currencyFormatter = currencyFormatter,
                        containerColor = SurfaceColor,
                        textColor = TextPrimary,
                        secondaryTextColor = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- UI COMPONENTS ---

@Composable
fun MonthYearPickerModern(
    selectedMonth: Int,
    selectedYear: Int,
    onDateSelected: (month: Int, year: Int) -> Unit,
    monthNames: List<String>
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, selectedMonth - 1)
        set(Calendar.YEAR, selectedYear)
    }
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, _: Int ->
            onDateSelected(month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SurfaceColor, RoundedCornerShape(25.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = {
                calendar.add(Calendar.MONTH, -1)
                onDateSelected(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
            },
            modifier = Modifier.size(42.dp).background(Color.Transparent, CircleShape)
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TextSecondary)
        }

        Row(
            modifier = Modifier.clickable { datePickerDialog.show() }.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        IconButton(
            onClick = {
                calendar.add(Calendar.MONTH, 1)
                onDateSelected(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
            },
            modifier = Modifier.size(42.dp).background(Color.Transparent, CircleShape)
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextSecondary)
        }
    }
}

@Composable
fun ChartTypeSelectorModern(selectedChartType: ChartType, onTypeSelected: (ChartType) -> Unit) {
    SegmentedControl(
        items = listOf(
            ChartType.BAR to stringResource(R.string.bars), // "Bar"
            ChartType.PIE to stringResource(R.string.cake)  // "Pie"
        ),
        selectedItem = selectedChartType,
        onItemSelected = onTypeSelected
    )
}

@Composable
fun TransactionFilterSelectorModern(selectedFilter: TransactionFilter, onFilterSelected: (TransactionFilter) -> Unit) {
    SegmentedControl(
        items = listOf(
            TransactionFilter.ALL to stringResource(R.string.all),
            TransactionFilter.INCOME to stringResource(R.string.income),
            TransactionFilter.EXPENSE to stringResource(R.string.expense)
        ),
        selectedItem = selectedFilter,
        onItemSelected = onFilterSelected
    )
}

@Composable
fun <T> SegmentedControl(
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SurfaceColor, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (item, label) ->
            val isSelected = item == selectedItem
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) PrimaryColor else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onItemSelected(item) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) OnPrimaryColor else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TransactionCardSmall(
    transaction: Any,
    categoryNameMap: Map<String, String>,
    currencyFormatter: NumberFormat,
    containerColor: Color = Color(0xFF1A1A1A),
    textColor: Color = Color.White,
    secondaryTextColor: Color = Color.Gray
) {
    val isIncome = transaction is Income
    val amount = if (isIncome) (transaction as Income).amount else (transaction as Expense).amount
    val description = if (isIncome) (transaction as Income).description else (transaction as Expense).description
    val rawCategory = if (isIncome) (transaction as Income).category else (transaction as Expense).category
    val category = categoryNameMap[rawCategory] ?: rawCategory
    val date = if (isIncome) (transaction as Income).date.toDate() else (transaction as Expense).date.toDate()
    val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
    val amountColor = if (isIncome) IncomeColor else ExpenseColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = description,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = category, color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
                Text(text = " • ", color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
                Text(text = dateFormatter.format(date), color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(
            text = currencyFormatter.format(amount),
            color = amountColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ImprovedPieChartModern(
    data: Map<String, Double>,
    categoryColors: Map<String, Color>,
    currencyFormatter: NumberFormat
) {
    val totalAmount = data.values.sum()
    if (data.isEmpty() || totalAmount == 0.0) return

    var selectedSlice by remember { mutableStateOf<String?>(null) }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.22f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val distance = sqrt(dx * dx + dy * dy)
                            val radius = min(size.width, size.height) / 2f

                            if (distance <= radius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                // Adjust angle to match startAngle of -90f
                                angle = (angle + 90f) % 360f

                                var currentAngle = 0f
                                data.entries.sortedByDescending { it.value }
                                    .forEach { entry ->
                                        val sweep = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
                                        if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                            selectedSlice = entry.key
                                        }
                                        currentAngle += sweep
                                    }
                            }
                        }
                    }
            ) {
                val diameter = min(size.width, size.height) * 0.9f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                var startAngle = -90f

                data.entries.sortedByDescending { it.value }
                    .forEach { entry ->
                        val sweepAngle = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
                        val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray }
                        val radiusMultiplier = if (selectedSlice == entry.key) 1.08f else 1f
                        val adjustedSize = Size(diameter * radiusMultiplier, diameter * radiusMultiplier)
                        val adjustedTopLeft = Offset(
                            topLeft.x - (diameter * (radiusMultiplier - 1f) / 2f),
                            topLeft.y - (diameter * (radiusMultiplier - 1f) / 2f)
                        )

                        // Draw Segment
                        drawArc(color = sectionColor, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, topLeft = adjustedTopLeft, size = adjustedSize)
                        // Draw Border/Separator
                        drawArc(color = SurfaceColor, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, topLeft = adjustedTopLeft, size = adjustedSize, style = Stroke(width = 2.dp.toPx()))
                        startAngle += sweepAngle
                    }
            }

            // Center Text Information (if slice selected)
            if (selectedSlice != null) {
                val amount = data[selectedSlice] ?: 0.0
                val percentage = ((amount / totalAmount) * 100).toInt()
                // Overlay text might obscure chart, usually good to place below or in center if it's a donut
                // Since this is a Pie (filled), we don't put text inside. Logic kept as requested structure.
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend / List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(data.entries.sortedByDescending { it.value }.toList()) { entry ->
                    val percentage = ((entry.value / totalAmount) * 100).toInt()
                    val isSelected = selectedSlice == entry.key

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) PrimaryColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { selectedSlice = if (isSelected) null else entry.key }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        val color = categoryColors.getOrElse(entry.key) { Color.Gray }
                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = entry.key, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = currencyFormatter.format(entry.value).replace("$", "").replace("COP", "").trim(), color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(text = "$percentage%", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if(!isSelected) HorizontalDivider(color = SurfaceColor.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionListDialog(
    transactions: List<Any>,
    label: String,
    categoryNameMap: Map<String, String>,
    currencyFormatter: NumberFormat,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                if (transactions.isEmpty()) {
                    Text(text = "No transactions", color = TextSecondary, modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(transactions) { transaction ->
                            TransactionCardSmall(
                                transaction = transaction,
                                categoryNameMap = categoryNameMap,
                                currencyFormatter = currencyFormatter,
                                containerColor = SurfaceColor,
                                textColor = TextPrimary,
                                secondaryTextColor = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PrimaryColor)
            }
        }
    )
}

@Composable
fun MonthlyBarChartImproved(
    data: List<MonthlySummary>,
    incomeColor: Color,
    expenseColor: Color,
    currencyFormatter: NumberFormat,
    monthNames: List<String>,
    incomeLabel: String,
    expenseLabel: String,
    onBarClick: (String, Int, Int, Boolean) -> Unit
) {
    if (data.isEmpty()) return
    val density = LocalDensity.current
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.parseColor("#B0B0C0")
            textAlign = Paint.Align.CENTER
            textSize = density.run { 10.sp.toPx() }
            typeface = Typeface.DEFAULT
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val chartWidth = size.width
                    val labelTextWidth = density.run { 65.dp.toPx() }
                    val drawableChartWidth = chartWidth - labelTextWidth
                    val barGroupWidth = drawableChartWidth / data.size
                    val singleBarWidth = barGroupWidth / 3

                    data.forEachIndexed { index, summary ->
                        val xOffset = labelTextWidth + index * barGroupWidth

                        // Check tap on Income Bar
                        val incomeBarX = xOffset + singleBarWidth / 2
                        if (offset.x >= incomeBarX && offset.x <= incomeBarX + singleBarWidth) {
                            val label = "$incomeLabel - ${monthNames.getOrElse(summary.month - 1) { "" }}"
                            onBarClick(label, summary.month, summary.year, true)
                        }

                        // Check tap on Expense Bar
                        val expenseBarX = xOffset + singleBarWidth / 2 + singleBarWidth + (singleBarWidth / 2)
                        if (offset.x >= expenseBarX && offset.x <= expenseBarX + singleBarWidth) {
                            val label = "$expenseLabel - ${monthNames.getOrElse(summary.month - 1) { "" }}"
                            onBarClick(label, summary.month, summary.year, false)
                        }
                    }
                }
            }
    ) {
        val chartWidth = size.width
        val chartHeight = size.height
        val maxAmount = (data.maxOfOrNull { it.totalIncome } ?: 0.0).coerceAtLeast(data.maxOfOrNull { it.totalExpense } ?: 0.0).coerceAtLeast(1.0)
        val numLabels = 4
        val labelInterval = maxAmount / numLabels
        val labelTextWidth = density.run { 65.dp.toPx() }
        val xAxisLabelHeight = density.run { 24.dp.toPx() }
        val drawableChartWidth = chartWidth - labelTextWidth
        val drawableChartHeight = chartHeight - xAxisLabelHeight
        val barGroupWidth = drawableChartWidth / data.size
        val singleBarWidth = barGroupWidth / 3

        // Draw Y-Axis Grid and Labels
        for (i in 0..numLabels) {
            val value = i * labelInterval
            val y = drawableChartHeight - (value / maxAmount).toFloat() * drawableChartHeight
            val label = currencyFormatter.format(value).replace("RSP", "R$").replace("COP", "").trim()
            if (i > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    labelTextWidth - 10,
                    y + textPaint.textSize / 3,
                    textPaint.apply { textAlign = Paint.Align.RIGHT }
                )
                drawLine(
                    color = SurfaceColor.copy(alpha = 0.5f),
                    start = Offset(labelTextWidth, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // Draw Bars
        data.forEachIndexed { index, summary ->
            val xOffset = labelTextWidth + index * barGroupWidth
            val incomeBarHeight = (summary.totalIncome / maxAmount).toFloat() * drawableChartHeight
            val expenseBarHeight = (summary.totalExpense / maxAmount).toFloat() * drawableChartHeight
            val barCornerRadius = CornerRadius(4.dp.toPx())

            // Income Bar
            val incomeBarX = xOffset + singleBarWidth / 2
            drawRoundRect(
                color = incomeColor,
                topLeft = Offset(incomeBarX, drawableChartHeight - incomeBarHeight),
                size = Size(singleBarWidth, incomeBarHeight),
                cornerRadius = barCornerRadius
            )

            // Expense Bar
            val expenseBarX = xOffset + singleBarWidth / 2 + singleBarWidth + (singleBarWidth / 2)
            drawRoundRect(
                color = expenseColor,
                topLeft = Offset(expenseBarX, drawableChartHeight - expenseBarHeight),
                size = Size(singleBarWidth, expenseBarHeight),
                cornerRadius = barCornerRadius
            )

            // X-Axis Month Label
            val monthLabel = monthNames.getOrElse(summary.month - 1) { "" }.take(3)
            drawContext.canvas.nativeCanvas.drawText(
                monthLabel,
                xOffset + barGroupWidth / 2,
                chartHeight - xAxisLabelHeight / 4,
                textPaint.apply { textAlign = Paint.Align.CENTER }
            )
        }

        // Legend
        val legendY = chartHeight + 20.dp.toPx()
        val legendBoxSize = density.run { 10.dp.toPx() }
        val legendPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#B0B0C0")
            textSize = density.run { 12.sp.toPx() }
            typeface = Typeface.DEFAULT
        }

        drawCircle(color = expenseColor, center = Offset(chartWidth / 2 - 60.dp.toPx(), legendY), radius = legendBoxSize/2)
        drawContext.canvas.nativeCanvas.drawText(expenseLabel, chartWidth / 2 - 50.dp.toPx() + 5.dp.toPx(), legendY + legendPaint.textSize / 3, legendPaint)

        drawCircle(color = incomeColor, center = Offset(chartWidth / 2 + 20.dp.toPx(), legendY), radius = legendBoxSize/2)
        drawContext.canvas.nativeCanvas.drawText(incomeLabel, chartWidth / 2 + 30.dp.toPx() + 5.dp.toPx(), legendY + legendPaint.textSize / 3, legendPaint)
    }
}