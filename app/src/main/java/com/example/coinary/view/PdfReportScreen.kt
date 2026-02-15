package com.example.coinary.view

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Debt
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.model.SavingsGoal
import com.example.coinary.viewmodel.TransactionFilter
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

// --- Consistent Color Palette ---
private val AppBackground = Color(0xFF0F0F1A)
private val SurfaceColor = Color(0xFF1E1E2E)
private val PrimaryColor = Color(0xFF5E5ADB)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0C0)

/**
 * PdfReportScreen: UI for configuring and generating PDF financial reports.
 * Allows users to select a specific month/year and filter transaction types (Income, Expense, All).
 * Generates the PDF using [PdfGenerator] and triggers a system share intent.
 *
 * @param navController Navigation controller for handling back navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReportScreen(
    navController: NavController
) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(color = AppBackground, darkIcons = false)
    }

    val context = LocalContext.current
    val firestoreManager = remember { FirestoreManager() }

    // --- State Management ---
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var isLoading by remember { mutableStateOf(false) }

    val monthNames = context.resources.getStringArray(R.array.months).toList()

    Scaffold(
        containerColor = AppBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER SECTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.background(SurfaceColor, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back_desc),
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.pdf_dialog_title), // "Generate PDF Report"
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- CENTRAL ICON ---
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.pdf_report_title),
                color = TextSecondary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- DATE SELECTOR (Month/Year) ---
            Text(
                text = stringResource(R.string.pdf_period_label).uppercase(), // "PERIOD:"
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            MonthYearPickerReport(
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                onDateSelected = { m, y -> selectedMonth = m; selectedYear = y },
                monthNames = monthNames
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- FILTER SELECTOR (Income/Expense/All) ---
            Text(
                text = stringResource(R.string.pdf_type_label).uppercase(), // "REPORT TYPE:"
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilterSelectorReport(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- GENERATE BUTTON ---
            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        generatePdfProcess(
                            context = context,
                            firestoreManager = firestoreManager,
                            month = selectedMonth,
                            year = selectedYear,
                            filter = selectedFilter,
                            monthNames = monthNames,
                            onComplete = { isLoading = false }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.pdf_generate_btn), // "Generate PDF"
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Orchestrates the PDF generation process: fetching data, filtering, creating the file, and triggering the share intent.
 */
private fun generatePdfProcess(
    context: Context,
    firestoreManager: FirestoreManager,
    month: Int,
    year: Int,
    filter: TransactionFilter,
    monthNames: List<String>,
    onComplete: () -> Unit
) {
    // 1. Fetch Transactions (Incomes and Expenses)
    firestoreManager.getIncomesByMonth(month, year, onSuccess = { incomes ->
        firestoreManager.getExpensesByMonth(month, year, onSuccess = { expenses ->

            // 2. Filter Data based on user selection
            val transactions: List<Any> = when (filter) {
                TransactionFilter.INCOME -> incomes
                TransactionFilter.EXPENSE -> expenses
                TransactionFilter.ALL -> (incomes + expenses).sortedByDescending { item ->
                    if (item is Income) item.date.toDate().time else (item as Expense).date.toDate().time
                }
            }

            if (transactions.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.pdf_no_data_toast), Toast.LENGTH_SHORT).show()
                onComplete()
                return@getExpensesByMonth
            }

            // 3. Fetch Savings and Debts (for comprehensive report)
            firestoreManager.getAllSavingsGoals(onSuccess = { savings ->
                firestoreManager.getAllDebts(onSuccess = { debts ->
                    createAndSharePdf(
                        context,
                        transactions,
                        savings,
                        debts,
                        month,
                        year,
                        filter,
                        monthNames
                    )
                    onComplete()
                }, onFailure = { onComplete() })
            }, onFailure = { onComplete() })

        }, onFailure = { onComplete() })
    }, onFailure = { onComplete() })
}

/**
 * Helper function to physically create the PDF file and open the share dialog.
 */
private fun createAndSharePdf(
    context: Context,
    transactions: List<Any>,
    savings: List<SavingsGoal>,
    debts: List<Debt>,
    month: Int,
    year: Int,
    filter: TransactionFilter,
    monthNames: List<String>
) {
    try {
        // 4. Generate PDF File
        val prefix = context.getString(R.string.pdf_file_prefix)
        val monthName = monthNames.getOrElse(month - 1) { "Unknown" }
        val fileName = "${prefix}_${monthName}_$year.pdf"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)

        val generator = PdfGenerator(context)

        val filterName = when (filter) {
            TransactionFilter.INCOME -> context.getString(R.string.report_filter_income)
            TransactionFilter.EXPENSE -> context.getString(R.string.report_filter_expense)
            TransactionFilter.ALL -> context.getString(R.string.report_filter_all)
        }

        generator.generateReport(
            outputStream,
            transactions,
            savings,
            debts,
            monthName,
            year.toString(),
            filterName
        )

        // 5. Share Intent
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.pdf_share_title)))

    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.pdf_error_msg, e.message), Toast.LENGTH_LONG).show()
    }
}

// --- UI COMPONENTS ---

@Composable
fun MonthYearPickerReport(
    selectedMonth: Int,
    selectedYear: Int,
    onDateSelected: (month: Int, year: Int) -> Unit,
    monthNames: List<String>
) {
    val context = LocalContext.current

    // Setup Calendar for DatePickerDialog
    val calendar = Calendar.getInstance().apply {
        set(Calendar.MONTH, selectedMonth - 1)
        set(Calendar.YEAR, selectedYear)
    }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, _ -> onDateSelected(month + 1, year) },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(SurfaceColor, RoundedCornerShape(16.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            // Logic for previous month button
            val newCal = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
            onDateSelected(newCal.get(Calendar.MONTH) + 1, newCal.get(Calendar.YEAR))
        }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TextSecondary)
        }

        Text(
            text = "${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { datePickerDialog.show() }
        )

        IconButton(onClick = {
            // Logic for next month button
            val newCal = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
            onDateSelected(newCal.get(Calendar.MONTH) + 1, newCal.get(Calendar.YEAR))
        }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextSecondary)
        }
    }
}

@Composable
fun FilterSelectorReport(
    selectedFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit
) {
    val options = listOf(
        TransactionFilter.ALL to stringResource(R.string.all),
        TransactionFilter.INCOME to stringResource(R.string.income),
        TransactionFilter.EXPENSE to stringResource(R.string.expense)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SurfaceColor, RoundedCornerShape(25.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { (filter, label) ->
            val isSelected = filter == selectedFilter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) PrimaryColor else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onFilterSelected(filter) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}