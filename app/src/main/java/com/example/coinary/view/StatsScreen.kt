package com.example.coinary.view

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * Main screen for displaying financial statistics, charts, and transaction history.
 * Handles PDF generation and data filtering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel = viewModel()
) {
    // --- System UI Configuration ---
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color(0xFF150F33)
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    val context = LocalContext.current

    // --- ViewModel State Collection ---
    val selectedMonth by statsViewModel.selectedMonth.collectAsState()
    val selectedYear by statsViewModel.selectedYear.collectAsState()
    val selectedChartType by statsViewModel.selectedChartType.collectAsState()
    val selectedTransactionFilter by statsViewModel.selectedTransactionFilter.collectAsState()

    val monthlyIncomes by statsViewModel.monthlyIncomes.collectAsState()
    val monthlyExpenses by statsViewModel.monthlyExpenses.collectAsState()
    val filteredTransactions by statsViewModel.filteredTransactions.collectAsState()
    val monthlySummaries by statsViewModel.monthlySummaries.collectAsState()

    var showPdfDialog by remember { mutableStateOf(false) }

    // Dynamic currency formatter based on the device's locale
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    // Resource arrays (automatically translated by Android based on locale)
    val monthNames = context.resources.getStringArray(R.array.months).toList()
    val expenseCategories = context.resources.getStringArray(R.array.expense_categories)
    val incomeCategories = context.resources.getStringArray(R.array.income_categories)

    // --- Translation Map: Database Value -> Current UI Language ---
    // This ensures that if data was saved as "Comida" but the device is now in English,
    // it properly displays "Food" in the UI.
    val categoryNameMap = remember(expenseCategories, incomeCategories) {
        mapOf(
            // Expense Translations
            "Comida" to expenseCategories[0], "Food" to expenseCategories[0], "Nourriture" to expenseCategories[0], "Alimentação" to expenseCategories[0],
            "Transporte" to expenseCategories[1], "Transport" to expenseCategories[1], "Transports" to expenseCategories[1],
            "Vivienda" to expenseCategories[2], "Housing" to expenseCategories[2], "Habitação" to expenseCategories[2], "Logement" to expenseCategories[2],
            "Ocio" to expenseCategories[3], "Entertainment" to expenseCategories[3], "Lazer" to expenseCategories[3], "Divertissement" to expenseCategories[3],
            "Servicios" to expenseCategories[4], "Services" to expenseCategories[4], "Serviços" to expenseCategories[4],
            "Compras" to expenseCategories[5], "Shopping" to expenseCategories[5], "Achats" to expenseCategories[5],
            "Salud" to expenseCategories[6], "Health" to expenseCategories[6], "Saúde" to expenseCategories[6], "Santé" to expenseCategories[6],
            "Educación" to expenseCategories[7], "Education" to expenseCategories[7], "Educação" to expenseCategories[7],
            "Otros Gastos" to expenseCategories[8], "Other Expenses" to expenseCategories[8], "Outros" to expenseCategories[8], "Autres" to expenseCategories[8],

            // Income Translations
            "Salario" to incomeCategories[0], "Salary" to incomeCategories[0], "Salário" to incomeCategories[0], "Salaire" to incomeCategories[0],
            "Regalo" to incomeCategories[1], "Gift" to incomeCategories[1], "Presente" to incomeCategories[1], "Cadeau" to incomeCategories[1],
            "Ventas" to incomeCategories[2], "Sales" to incomeCategories[2], "Vendas" to incomeCategories[2], "Ventes" to incomeCategories[2],
            "Inversión" to incomeCategories[3], "Investment" to incomeCategories[3], "Investimento" to incomeCategories[3], "Investissement" to incomeCategories[3],
            "Otros Ingresos" to incomeCategories[4], "Other Income" to incomeCategories[4], "Outros" to incomeCategories[4], "Autres revenus" to incomeCategories[4]
        )
    }

    // Map associating specific categories with their respective colors
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
            expenseCategories[8] to Color(0xFF03A9F4), // Other Expenses

            incomeCategories[0] to Color(0xFF33CC33), // Salary
            incomeCategories[1] to Color(0xFF800080), // Gift
            incomeCategories[2] to Color(0xFFFFA500), // Sales
            incomeCategories[3] to Color(0xFF00CED1), // Investment
            incomeCategories[4] to Color(0xFFC0C0C0)  // Other Income
        )
    }

    Scaffold(
        containerColor = Color.Black,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPdfDialog = true },
                containerColor = Color(0xFF4D54BF),
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "PDF")
            }
        }
    ) { paddingValues ->

        // --- PDF Generation Dialog Logic ---
        if (showPdfDialog) {
            PdfOptionsDialog(
                onDismiss = { showPdfDialog = false },
                onGenerate = { filterOption ->
                    showPdfDialog = false
                    val dataToPrint: List<Any> = when (filterOption) {
                        TransactionFilter.INCOME -> monthlyIncomes
                        TransactionFilter.EXPENSE -> monthlyExpenses
                        TransactionFilter.ALL -> (monthlyIncomes + monthlyExpenses).sortedByDescending {
                            if (it is Income) it.date.toDate().time else (it as Expense).date.toDate().time
                        }
                    }

                    if (dataToPrint.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.pdf_no_data_toast), Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            // Define localized file name
                            // Example: Coinary_Report_January_2026.pdf
                            val filePrefix = context.getString(R.string.pdf_file_prefix)
                            val fileName = "${filePrefix}_${monthNames[selectedMonth - 1]}_$selectedYear.pdf"
                            val file = File(context.cacheDir, fileName)
                            val outputStream = FileOutputStream(file)

                            val filterName = when (filterOption) {
                                TransactionFilter.INCOME -> context.getString(R.string.report_filter_income)
                                TransactionFilter.EXPENSE -> context.getString(R.string.report_filter_expense)
                                TransactionFilter.ALL -> context.getString(R.string.report_filter_all)
                            }

                            // Initialize PDF Generator
                            val generator = PdfGenerator(context)
                            generator.generateReport(
                                outputStream,
                                dataToPrint,
                                monthNames[selectedMonth - 1],
                                selectedYear.toString(),
                                filterName
                            )

                            // Create Intent to share the file
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.pdf_share_title)))

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, context.getString(R.string.pdf_error_msg, e.message), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- 1. Header Section ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- 2. Month/Year Picker ---
            item {
                MonthYearPicker(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onDateSelected = { month, year -> statsViewModel.updateSelectedDate(month, year) },
                    monthNames = monthNames
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- 3. Chart Type Selector ---
            item {
                ChartTypeSelector(selectedChartType = selectedChartType) { type ->
                    statsViewModel.updateChartType(type)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- 4. Chart Visualization Container ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                        // Determine which transactions to display based on the filter
                        val transactionsForDetail = when (selectedTransactionFilter) {
                            TransactionFilter.INCOME -> monthlyIncomes
                            TransactionFilter.EXPENSE -> monthlyExpenses
                            TransactionFilter.ALL -> if (monthlyExpenses.isNotEmpty()) monthlyExpenses else monthlyIncomes
                        }

                        // Render appropriate chart
                        when (selectedChartType) {
                            ChartType.BAR -> {
                                MonthlyBarChart(
                                    data = monthlySummaries,
                                    incomeColor = Color(0xFF33CC33),
                                    expenseColor = Color(0xFFE91E63),
                                    currencyFormatter = currencyFormatter,
                                    monthNames = monthNames,
                                    incomeLabel = stringResource(R.string.legend_income),
                                    expenseLabel = stringResource(R.string.legend_expense)
                                )
                            }
                            else -> {
                                val rawData = statsViewModel.getCategorizedTotalsForPieChart(transactionsForDetail as List<Any>)
                                // Translate map keys before rendering the chart
                                val translatedData = rawData.mapKeys { (key, _) -> categoryNameMap[key] ?: key }
                                PieChart(data = translatedData, categoryColors = categoryColorMap, currencyFormatter = currencyFormatter)
                            }
                        }

                        // Handle "No Data" State
                        val noData = when (selectedChartType) {
                            ChartType.BAR -> monthlySummaries.isEmpty()
                            else -> transactionsForDetail.isEmpty()
                        }
                        if (noData) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(text = stringResource(R.string.no_data_selected_month), color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                                if (selectedTransactionFilter == TransactionFilter.ALL && monthlyIncomes.isEmpty() && monthlyExpenses.isEmpty()) {
                                    Text(text = stringResource(R.string.no_transactions_found), color = Color.DarkGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- 5. Transaction Filter Chips ---
            item {
                TransactionFilterSelector(selectedFilter = selectedTransactionFilter) { filter ->
                    statsViewModel.updateTransactionFilter(filter)
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- 6. Transaction List ---
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.no_data_selected_month), color = Color.Gray, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    val originalCategory = if (transaction is Income) transaction.category else (transaction as Expense).category
                    // Translate category for list item
                    val translatedCategory = categoryNameMap[originalCategory] ?: originalCategory

                    TransactionItem(transaction = transaction, displayCategory = translatedCategory, currencyFormatter = currencyFormatter)
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- AUXILIARY COMPONENTS ---

/**
 * Dialog component to select transaction filters before PDF generation.
 */
@Composable
fun PdfOptionsDialog(onDismiss: () -> Unit, onGenerate: (TransactionFilter) -> Unit) {
    var selectedOption by remember { mutableStateOf(TransactionFilter.ALL) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pdf_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.pdf_select_option))
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedOption = TransactionFilter.ALL }.padding(vertical = 4.dp)) {
                    RadioButton(selected = selectedOption == TransactionFilter.ALL, onClick = { selectedOption = TransactionFilter.ALL })
                    Text(stringResource(R.string.pdf_option_all))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedOption = TransactionFilter.INCOME }.padding(vertical = 4.dp)) {
                    RadioButton(selected = selectedOption == TransactionFilter.INCOME, onClick = { selectedOption = TransactionFilter.INCOME })
                    Text(stringResource(R.string.pdf_option_income))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedOption = TransactionFilter.EXPENSE }.padding(vertical = 4.dp)) {
                    RadioButton(selected = selectedOption == TransactionFilter.EXPENSE, onClick = { selectedOption = TransactionFilter.EXPENSE })
                    Text(stringResource(R.string.pdf_option_expense))
                }
            }
        },
        confirmButton = { Button(onClick = { onGenerate(selectedOption) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF))) { Text(stringResource(R.string.pdf_generate_btn)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pdf_cancel_btn)) } }
    )
}

/**
 * Composable representing a single transaction row in the list.
 */
@Composable
fun TransactionItem(transaction: Any, displayCategory: String, currencyFormatter: NumberFormat) {
    val isIncome = transaction is Income
    val amount = if (isIncome) (transaction as Income).amount else (transaction as Expense).amount
    val description = if (isIncome) (transaction as Income).description else (transaction as Expense).description
    val date = if (isIncome) (transaction as Income).date.toDate() else (transaction as Expense).date.toDate()
    val amountColor = if (isIncome) Color(0xFF33CC33) else Color(0xFFE91E63)
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .background(Color(0xFF3A3A3A), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = description, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = displayCategory, color = Color.Gray, fontSize = 14.sp)
            Text(text = dateFormatter.format(date), color = Color.Gray, fontSize = 12.sp)
        }
        Text(text = currencyFormatter.format(amount), color = amountColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun MonthYearPicker(selectedMonth: Int, selectedYear: Int, onDateSelected: (month: Int, year: Int) -> Unit, monthNames: List<String>) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { set(Calendar.MONTH, selectedMonth - 1); set(Calendar.YEAR, selectedYear) }
    val datePickerDialog = DatePickerDialog(context, { _: DatePicker, year: Int, month: Int, _: Int -> onDateSelected(month + 1, year) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { calendar.add(Calendar.MONTH, -1); onDateSelected(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF))) { Text("<", color = Color.White) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = "${monthNames[selectedMonth - 1]} $selectedYear", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { datePickerDialog.show() })
        Spacer(modifier = Modifier.width(16.dp))
        Button(onClick = { calendar.add(Calendar.MONTH, 1); onDateSelected(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF))) { Text(">", color = Color.White) }
    }
}

@Composable
fun ChartTypeSelector(selectedChartType: ChartType, onTypeSelected: (ChartType) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        val context = LocalContext.current
        FilterChip(selected = selectedChartType == ChartType.BAR, onClick = { onTypeSelected(ChartType.BAR) }, label = { Text(stringResource(R.string.bars), color = if (selectedChartType == ChartType.BAR) Color.Black else Color.White) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF2E423), containerColor = Color(0xFF4D54BF)))
        Spacer(modifier = Modifier.width(16.dp))
        FilterChip(selected = selectedChartType == ChartType.PIE, onClick = { onTypeSelected(ChartType.PIE) }, label = { Text(stringResource(R.string.cake), color = if (selectedChartType == ChartType.PIE) Color.Black else Color.White) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF2E423), containerColor = Color(0xFF4D54BF)))
    }
}

@Composable
fun TransactionFilterSelector(selectedFilter: TransactionFilter, onFilterSelected: (TransactionFilter) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
        FilterChip(selected = selectedFilter == TransactionFilter.ALL, onClick = { onFilterSelected(TransactionFilter.ALL) }, label = { Text(stringResource(R.string.all), color = if (selectedFilter == TransactionFilter.ALL) Color.Black else Color.White) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF2E423), containerColor = Color(0xFF4D54BF)))
        FilterChip(selected = selectedFilter == TransactionFilter.INCOME, onClick = { onFilterSelected(TransactionFilter.INCOME) }, label = { Text(stringResource(R.string.income), color = if (selectedFilter == TransactionFilter.INCOME) Color.Black else Color.White) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF2E423), containerColor = Color(0xFF4D54BF)))
        FilterChip(selected = selectedFilter == TransactionFilter.EXPENSE, onClick = { onFilterSelected(TransactionFilter.EXPENSE) }, label = { Text(stringResource(R.string.expense), color = if (selectedFilter == TransactionFilter.EXPENSE) Color.Black else Color.White) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFF2E423), containerColor = Color(0xFF4D54BF)))
    }
}

/**
 * Renders a Pie Chart using Canvas.
 */
@Composable
fun PieChart(data: Map<String, Double>, categoryColors: Map<String, Color>, currencyFormatter: NumberFormat) {
    val totalAmount = data.values.sum()
    if (data.isEmpty() || totalAmount == 0.0) return
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = min(size.width, size.height)
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val rect = Size(diameter, diameter)
                var startAngle = -90f
                data.entries.sortedByDescending { it.value }.forEach { entry ->
                    val sweepAngle = (entry.value.toFloat() / totalAmount.toFloat()) * 360f
                    val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray }
                    drawArc(color = sectionColor, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, topLeft = topLeft, size = rect)
                    drawArc(color = Color(0xFF2C2C2C), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = true, topLeft = topLeft, size = rect, style = Stroke(width = 2.dp.toPx()))
                    startAngle += sweepAngle
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            data.entries.sortedByDescending { it.value }.forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    val color = categoryColors.getOrElse(entry.key) { Color.Gray }
                    Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = entry.key, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(text = currencyFormatter.format(entry.value).replace("RSP", "R$"), color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Renders a Bar Chart comparing Income vs. Expenses.
 */
@Composable
fun MonthlyBarChart(
    data: List<MonthlySummary>,
    incomeColor: Color,
    expenseColor: Color,
    currencyFormatter: NumberFormat,
    monthNames: List<String>,
    incomeLabel: String,
    expenseLabel: String
) {
    if (data.isEmpty()) return
    val density = LocalDensity.current
    val textPaint = remember { Paint().apply { color = android.graphics.Color.WHITE; textAlign = Paint.Align.CENTER; textSize = density.run { 10.sp.toPx() }; typeface = Typeface.DEFAULT_BOLD } }

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
        val chartWidth = size.width
        val chartHeight = size.height
        val maxAmount = (data.maxOfOrNull { it.totalIncome } ?: 0.0).coerceAtLeast(data.maxOfOrNull { it.totalExpense } ?: 0.0).coerceAtLeast(1.0)
        val numLabels = 4
        val labelInterval = maxAmount / numLabels
        val labelTextWidth = density.run { 60.dp.toPx() }
        val xAxisLabelHeight = density.run { 20.dp.toPx() }
        val drawableChartWidth = chartWidth - labelTextWidth
        val drawableChartHeight = chartHeight - xAxisLabelHeight
        val barGroupWidth = drawableChartWidth / data.size
        val singleBarWidth = barGroupWidth / 3

        for (i in 0..numLabels) {
            val value = i * labelInterval
            val y = drawableChartHeight - (value / maxAmount).toFloat() * drawableChartHeight
            val label = currencyFormatter.format(value).replace("RSP", "R$")
            drawContext.canvas.nativeCanvas.drawText(label, labelTextWidth / 2, y + textPaint.textSize / 3, textPaint)
            drawLine(color = Color.Gray.copy(alpha = 0.3f), start = Offset(labelTextWidth, y), end = Offset(chartWidth, y), strokeWidth = 1.dp.toPx())
        }
        data.forEachIndexed { index, summary ->
            val xOffset = labelTextWidth + index * barGroupWidth
            val incomeBarHeight = (summary.totalIncome / maxAmount).toFloat() * drawableChartHeight
            val expenseBarHeight = (summary.totalExpense / maxAmount).toFloat() * drawableChartHeight
            drawRect(color = incomeColor, topLeft = Offset(xOffset + singleBarWidth / 2, drawableChartHeight - incomeBarHeight), size = Size(singleBarWidth, incomeBarHeight))
            drawRect(color = expenseColor, topLeft = Offset(xOffset + singleBarWidth / 2 + singleBarWidth + (singleBarWidth / 2), drawableChartHeight - expenseBarHeight), size = Size(singleBarWidth, expenseBarHeight))
            val monthLabel = monthNames.getOrElse(summary.month - 1) { "" }
            drawContext.canvas.nativeCanvas.drawText(monthLabel, xOffset + barGroupWidth / 2, chartHeight + xAxisLabelHeight / 2, textPaint)
        }

        // Legend drawing
        val legendY = chartHeight + xAxisLabelHeight + 10.dp.toPx()
        val legendBoxSize = density.run { 10.dp.toPx() }
        val legendTextSize = density.run { 12.sp.toPx() }
        val legendPaint = Paint().apply { color = android.graphics.Color.WHITE; textSize = legendTextSize }

        drawRect(color = expenseColor, topLeft = Offset(chartWidth / 2 - 80.dp.toPx(), legendY), size = Size(legendBoxSize, legendBoxSize))
        drawContext.canvas.nativeCanvas.drawText(expenseLabel, chartWidth / 2 - 80.dp.toPx() + legendBoxSize + density.run { 5.dp.toPx() }, legendY + legendBoxSize / 2 + legendTextSize / 3, legendPaint)

        drawRect(color = incomeColor, topLeft = Offset(chartWidth / 2 + 10.dp.toPx(), legendY), size = Size(legendBoxSize, legendBoxSize))
        drawContext.canvas.nativeCanvas.drawText(incomeLabel, chartWidth / 2 + 10.dp.toPx() + legendBoxSize + density.run { 5.dp.toPx() }, legendY + legendBoxSize / 2 + legendTextSize / 3, legendPaint)
    }
}