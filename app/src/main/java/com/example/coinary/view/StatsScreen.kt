package com.example.coinary.view

import android.app.DatePickerDialog
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.DatePicker
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import kotlin.math.min
import com.example.coinary.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel = viewModel()
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
    val selectedMonth by statsViewModel.selectedMonth.collectAsState()
    val selectedYear by statsViewModel.selectedYear.collectAsState()
    val selectedChartType by statsViewModel.selectedChartType.collectAsState()
    val selectedTransactionFilter by statsViewModel.selectedTransactionFilter.collectAsState()

    val monthlyIncomes by statsViewModel.monthlyIncomes.collectAsState()
    val monthlyExpenses by statsViewModel.monthlyExpenses.collectAsState()
    val filteredTransactions by statsViewModel.filteredTransactions.collectAsState()
    val monthlySummaries by statsViewModel.monthlySummaries.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    val monthNames = remember {
        listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
    }

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
            expenseCategories[8] to Color(0xFF03A9F4), // Other Expenses

            incomeCategories[0] to Color(0xFF33CC33), // Salary
            incomeCategories[1] to Color(0xFF800080), // Gift
            incomeCategories[2] to Color(0xFFFFA500), // Sales
            incomeCategories[3] to Color(0xFF00CED1), // Investment
            incomeCategories[4] to Color(0xFFC0C0C0)  // Other Income
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
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
                    text = "Estadísticas Mensuales",
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

        item {
            MonthYearPicker(
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onDateSelected = { month, year -> statsViewModel.updateSelectedDate(month, year) },
                monthNames = monthNames
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            ChartTypeSelector(selectedChartType = selectedChartType) { type ->
                statsViewModel.updateChartType(type)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp), // Aumentado la altura de la Card
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (selectedChartType) {
                        ChartType.BAR -> {
                            MonthlyBarChart(
                                data = monthlySummaries,
                                incomeColor = Color(0xFF33CC33),
                                expenseColor = Color(0xFFE91E63),
                                currencyFormatter = currencyFormatter,
                                monthNames = monthNames
                            )
                        }
                        ChartType.LINE -> {
                            val data = statsViewModel.getDailyTotalsForChart(monthlyExpenses)
                            LineChart(data = data, lineColor = Color(0xFF00BCD4), currencyFormatter = currencyFormatter)
                        }
                        ChartType.PIE -> {
                            val data = statsViewModel.getCategorizedTotalsForPieChart(monthlyExpenses)
                            PieChart(data = data, categoryColors = categoryColorMap, currencyFormatter = currencyFormatter)
                        }
                    }
                    if (monthlyExpenses.isEmpty() && monthlyIncomes.isEmpty() && monthlySummaries.isEmpty()) {
                        Text(
                            text = "No hay datos para el mes seleccionado.",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            TransactionFilterSelector(selectedFilter = selectedTransactionFilter) { filter ->
                statsViewModel.updateTransactionFilter(filter)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (filteredTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay transacciones para este filtro y mes.",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredTransactions) { transaction ->
                TransactionItem(transaction = transaction, currencyFormatter = currencyFormatter)
            }
        }
    }
}

@Composable
fun MonthYearPicker(
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = {
                calendar.add(Calendar.MONTH, -1)
                onDateSelected(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF))
        ) {
            Text("<", color = Color.White)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "${monthNames[selectedMonth - 1]} $selectedYear",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { datePickerDialog.show() }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Button(
            onClick = {
                calendar.add(Calendar.MONTH, 1)
                onDateSelected(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D54BF))
        ) {
            Text(">", color = Color.White)
        }
    }
}

@Composable
fun ChartTypeSelector(selectedChartType: ChartType, onTypeSelected: (ChartType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        FilterChip(
            selected = selectedChartType == ChartType.BAR,
            onClick = { onTypeSelected(ChartType.BAR) },
            label = { Text("Barras", color = if (selectedChartType == ChartType.BAR) Color.Black else Color.White) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFF2E423),
                containerColor = Color(0xFF4D54BF)
            )
        )
        FilterChip(
            selected = selectedChartType == ChartType.LINE,
            onClick = { onTypeSelected(ChartType.LINE) },
            label = { Text("Líneas", color = if (selectedChartType == ChartType.LINE) Color.Black else Color.White) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFF2E423),
                containerColor = Color(0xFF4D54BF)
            )
        )
        FilterChip(
            selected = selectedChartType == ChartType.PIE,
            onClick = { onTypeSelected(ChartType.PIE) },
            label = { Text("Torta", color = if (selectedChartType == ChartType.PIE) Color.Black else Color.White) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFF2E423),
                containerColor = Color(0xFF4D54BF)
            )
        )
    }
}

@Composable
fun TransactionFilterSelector(selectedFilter: TransactionFilter, onFilterSelected: (TransactionFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        FilterChip(
            selected = selectedFilter == TransactionFilter.ALL,
            onClick = { onFilterSelected(TransactionFilter.ALL) },
            label = { Text("Todos", color = if (selectedFilter == TransactionFilter.ALL) Color.Black else Color.White) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFF2E423),
                containerColor = Color(0xFF4D54BF)
            )
        )
        FilterChip(
            selected = selectedFilter == TransactionFilter.INCOME,
            onClick = { onFilterSelected(TransactionFilter.INCOME) },
            label = { Text("Ingresos", color = if (selectedFilter == TransactionFilter.INCOME) Color.Black else Color.White) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFF2E423),
                containerColor = Color(0xFF4D54BF)
            )
        )
        FilterChip(
            selected = selectedFilter == TransactionFilter.EXPENSE,
            onClick = { onFilterSelected(TransactionFilter.EXPENSE) },
            label = { Text("Gastos", color = if (selectedFilter == TransactionFilter.EXPENSE) Color.Black else Color.White) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFF2E423),
                containerColor = Color(0xFF4D54BF)
            )
        )
    }
}

@Composable
fun TransactionItem(transaction: Any, currencyFormatter: NumberFormat) {
    val isIncome = transaction is Income
    val amount = if (isIncome) (transaction as Income).amount else (transaction as Expense).amount
    val description = if (isIncome) (transaction as Income).description else (transaction as Expense).description
    val category = if (isIncome) (transaction as Income).category else (transaction as Expense).category
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
            Text(
                text = description,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = category,
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = dateFormatter.format(date),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        Text(
            text = currencyFormatter.format(amount),
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun BarChart(data: Map<Int, Double>, barColor: Color) {
    if (data.isEmpty()) {
        Text("No hay datos para la gráfica de barras.", color = Color.Gray, modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center))
        return
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val maxAmount = data.values.maxOrNull() ?: 1.0
        val barWidth = size.width / (data.size * 2)
        val spacing = barWidth / 2
        val usableHeight = size.height - 20.dp.toPx()

        var currentX = spacing

        data.forEach { (day, amount) ->
            val barHeight = (amount / maxAmount).toFloat() * usableHeight
            drawRect(
                color = barColor,
                topLeft = Offset(currentX, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )

            drawContext.canvas.nativeCanvas.drawText(
                day.toString(),
                currentX + barWidth / 2,
                size.height + 10.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = 12.sp.toPx()
                }
            )
            currentX += barWidth + spacing
        }
    }
}

@Composable
fun LineChart(data: Map<Int, Double>, lineColor: Color, currencyFormatter: NumberFormat) {
    if (data.isEmpty()) {
        Text("No hay datos para la gráfica de líneas.", color = Color.Gray, modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center))
        return
    }

    val density = LocalDensity.current
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = density.run { 10.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val axisPaint = remember {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = density.run { 1.dp.toPx() }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
        val chartWidth = size.width
        val chartHeight = size.height

        val maxAmount = data.values.maxOrNull() ?: 1.0
        val minX = data.keys.minOrNull()?.toFloat() ?: 1f
        val maxX = data.keys.maxOrNull()?.toFloat() ?: 31f // Asume hasta 31 días
        val usableHeight = chartHeight - density.run { 40.dp.toPx() } // Espacio para etiquetas X y Y

        val labelTextWidth = density.run { 60.dp.toPx() } // Ancho para etiquetas del eje Y
        val xAxisLabelHeight = density.run { 20.dp.toPx() } // Altura para etiquetas del eje X

        val drawableChartWidth = chartWidth - labelTextWidth
        val drawableChartHeight = chartHeight - xAxisLabelHeight

        // Dibujar etiquetas y líneas del eje Y
        val numLabelsY = 4
        val labelIntervalY = maxAmount / numLabelsY
        for (i in 0..numLabelsY) {
            val value = i * labelIntervalY
            val y = drawableChartHeight - (value / maxAmount).toFloat() * usableHeight
            val label = currencyFormatter.format(value).replace("RSP", "R$")

            drawContext.canvas.nativeCanvas.drawText(
                label,
                labelTextWidth / 2,
                y + textPaint.textSize / 3,
                textPaint
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(labelTextWidth, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val points = data.entries.sortedBy { it.key }.map { (day, amount) ->
            val x = labelTextWidth + (day - minX) / (maxX - minX) * drawableChartWidth
            val y = drawableChartHeight - (amount / maxAmount).toFloat() * usableHeight
            Offset(x, y.toFloat())
        }

        if (points.size > 1) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        points.forEachIndexed { index, offset ->
            drawCircle(
                color = lineColor,
                center = offset,
                radius = 5.dp.toPx()
            )
            // Dibujar etiqueta del día
            val dayLabel = data.keys.sorted()[index].toString()
            drawContext.canvas.nativeCanvas.drawText(
                dayLabel,
                offset.x,
                chartHeight - xAxisLabelHeight / 2 + textPaint.textSize / 3, // Posición debajo del punto, ajustado para el eje X
                textPaint
            )
        }
    }
}

@Composable
fun PieChart(data: Map<String, Double>, categoryColors: Map<String, Color>, currencyFormatter: NumberFormat) {
    val totalAmount = data.values.sum()
    if (data.isEmpty() || totalAmount == 0.0) {
        Text("No hay datos para la gráfica de torta.", color = Color.Gray, modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center))
        return
    }

    val density = LocalDensity.current
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = density.run { 12.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val smallTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            textAlign = Paint.Align.CENTER
            textSize = density.run { 10.sp.toPx() }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val chartWidth = size.width
        val chartHeight = size.height
        val legendHeight = density.run { (data.size * 20).dp.toPx() } // Estimación de altura para la leyenda
        val usableChartHeight = chartHeight - legendHeight - density.run { 20.dp.toPx() } // Espacio para la leyenda

        val diameter = min(chartWidth, usableChartHeight) * 1.0f // Aumentado el tamaño de la torta a 1.0f
        val topLeft = Offset(
            (chartWidth - diameter) / 2f,
            (usableChartHeight - diameter) / 2f
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
                useCenter = true,
                topLeft = topLeft,
                size = rect
            )
            // Dibujar borde negro (antes blanco para contraste con fondo claro, ahora negro para contraste con rebanadas)
            drawArc(
                color = Color.Black, // Color del borde
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = topLeft,
                size = rect,
                style = Stroke(width = density.run { 2.dp.toPx() }) // Ancho del borde
            )

            // Se eliminaron las llamadas a drawContext.canvas.nativeCanvas.drawText para el texto sobre las rebanadas

            startAngle += sweepAngle
        }

        // Dibujar Leyenda
        val legendStartX = density.run { 16.dp.toPx() }
        var currentLegendY = usableChartHeight + density.run { 20.dp.toPx() } // Posición inicial de la leyenda

        data.entries.sortedByDescending { it.value }.forEach { entry ->
            val sectionColor = categoryColors.getOrElse(entry.key) { Color.Gray }
            val formattedAmount = currencyFormatter.format(entry.value).replace("RSP", "R$")

            // Rectángulo de color en la leyenda
            drawRect(
                color = sectionColor,
                topLeft = Offset(legendStartX, currentLegendY),
                size = Size(density.run { 10.dp.toPx() }, density.run { 10.dp.toPx() })
            )

            // Texto de la leyenda (Categoría y Monto)
            drawContext.canvas.nativeCanvas.drawText(
                "${entry.key}: $formattedAmount",
                legendStartX + density.run { 15.dp.toPx() },
                currentLegendY + density.run { 8.dp.toPx() }, // Ajuste para alinear con el centro del cuadrado
                textPaint.apply { textAlign = Paint.Align.LEFT }
            )
            currentLegendY += density.run { 20.dp.toPx() } // Espacio para la siguiente entrada de la leyenda
        }
    }
}

@Composable
fun MonthlyBarChart(
    data: List<MonthlySummary>,
    incomeColor: Color,
    expenseColor: Color,
    currencyFormatter: NumberFormat,
    monthNames: List<String>
) {
    if (data.isEmpty()) {
        Text(
            "No hay datos para la gráfica de barras mensual.",
            color = Color.Gray,
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
        )
        return
    }

    val density = LocalDensity.current
    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = density.run { 10.sp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    val axisPaint = remember {
        Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = density.run { 1.dp.toPx() }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp)) {
        val chartWidth = size.width
        val chartHeight = size.height

        val maxAmount = (data.maxOfOrNull { it.totalIncome } ?: 0.0)
            .coerceAtLeast(data.maxOfOrNull { it.totalExpense } ?: 0.0)
            .coerceAtLeast(1.0)

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

            drawContext.canvas.nativeCanvas.drawText(
                label,
                labelTextWidth / 2,
                y + textPaint.textSize / 3,
                textPaint
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(labelTextWidth, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        data.forEachIndexed { index, summary ->
            val xOffset = labelTextWidth + index * barGroupWidth
            val incomeBarHeight = (summary.totalIncome / maxAmount).toFloat() * drawableChartHeight
            val expenseBarHeight = (summary.totalExpense / maxAmount).toFloat() * drawableChartHeight

            drawRect(
                color = incomeColor,
                topLeft = Offset(xOffset + singleBarWidth / 2, drawableChartHeight - incomeBarHeight),
                size = Size(singleBarWidth, incomeBarHeight)
            )

            drawRect(
                color = expenseColor,
                topLeft = Offset(xOffset + singleBarWidth / 2 + singleBarWidth + (singleBarWidth / 2), drawableChartHeight - expenseBarHeight),
                size = Size(singleBarWidth, expenseBarHeight)
            )

            val monthLabel = monthNames[summary.month - 1]
            drawContext.canvas.nativeCanvas.drawText(
                monthLabel,
                xOffset + barGroupWidth / 2,
                chartHeight + xAxisLabelHeight / 2,
                textPaint
            )
        }

        val legendY = chartHeight + xAxisLabelHeight + 10.dp.toPx()
        val legendBoxSize = density.run { 10.dp.toPx() }
        val legendTextSize = density.run { 12.sp.toPx() }
        val legendPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = legendTextSize
        }

        drawRect(
            color = expenseColor,
            topLeft = Offset(chartWidth / 2 - 80.dp.toPx(), legendY),
            size = Size(legendBoxSize, legendBoxSize)
        )
        drawContext.canvas.nativeCanvas.drawText(
            "Gastos",
            chartWidth / 2 - 80.dp.toPx() + legendBoxSize + density.run { 5.dp.toPx() },
            legendY + legendBoxSize / 2 + legendTextSize / 3,
            legendPaint
        )

        drawRect(
            color = incomeColor,
            topLeft = Offset(chartWidth / 2 + 10.dp.toPx(), legendY),
            size = Size(legendBoxSize, legendBoxSize)
        )
        drawContext.canvas.nativeCanvas.drawText(
            "Ingresos",
            chartWidth / 2 + 10.dp.toPx() + legendBoxSize + density.run { 5.dp.toPx() },
            legendY + legendBoxSize / 2 + legendTextSize / 3,
            legendPaint
        )
    }
}
