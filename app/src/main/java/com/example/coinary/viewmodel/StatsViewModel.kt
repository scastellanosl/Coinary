package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Enum for Chart Types supported in the Stats Screen.
 */
enum class ChartType {
    BAR, PIE
}

/**
 * Enum for filtering the transaction list.
 */
enum class TransactionFilter {
    ALL, INCOME, EXPENSE
}

/**
 * Data class representing a monthly financial summary.
 * Used for populating the Bar Chart.
 */
data class MonthlySummary(
    val month: Int, // 1-12
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double
)

/**
 * StatsViewModel: Manages the state and data fetching for the Statistics Screen.
 * Handles the aggregation of monthly data for charts and the filtered list of transactions.
 *
 * @param firestoreManager Repository class for Firestore operations.
 */
class StatsViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    // --- State: Date Selection ---
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    // --- State: UI Controls ---
    private val _selectedChartType = MutableStateFlow(ChartType.BAR)
    val selectedChartType: StateFlow<ChartType> = _selectedChartType

    private val _selectedTransactionFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedTransactionFilter: StateFlow<TransactionFilter> = _selectedTransactionFilter

    // --- State: Monthly Data (Real-time for Pie Chart & List) ---
    private val _monthlyIncomes = MutableStateFlow<List<Income>>(emptyList())
    val monthlyIncomes: StateFlow<List<Income>> = _monthlyIncomes

    private val _monthlyExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val monthlyExpenses: StateFlow<List<Expense>> = _monthlyExpenses

    // --- State: Multi-Month Summary (One-time fetch for Bar Chart) ---
    // Holds data for the selected month and the 2 preceding months.
    private val _monthlySummaries = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val monthlySummaries: StateFlow<List<MonthlySummary>> = _monthlySummaries

    // --- Derived State: Filtered Transaction List ---
    // Combines Incomes and Expenses, sorts by date descending, and applies the selected filter.
    val filteredTransactions: StateFlow<List<Any>> = combine(
        _monthlyIncomes,
        _monthlyExpenses,
        _selectedTransactionFilter
    ) { incomes, expenses, filter ->
        when (filter) {
            TransactionFilter.ALL -> (incomes + expenses).sortedByDescending {
                when (it) {
                    is Income -> it.date.toDate()
                    is Expense -> it.date.toDate()
                    else -> Date(0) // Fallback for safety
                }
            }
            TransactionFilter.INCOME -> incomes.sortedByDescending { it.date.toDate() }
            TransactionFilter.EXPENSE -> expenses.sortedByDescending { it.date.toDate() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Initial data load
        fetchMonthlyData()      // Current month details (Real-time)
        fetchChartDataForMonths() // 3-month summary (One-time)
    }

    /**
     * Updates the selected date and refreshes all data sources.
     *
     * @param month The new month (1-12).
     * @param year The new year.
     */
    fun updateSelectedDate(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year

        fetchMonthlyData()
        fetchChartDataForMonths()
    }

    /**
     * Updates the currently displayed chart type.
     */
    fun updateChartType(chartType: ChartType) {
        _selectedChartType.value = chartType
    }

    /**
     * Updates the filter applied to the transaction list.
     */
    fun updateTransactionFilter(filter: TransactionFilter) {
        _selectedTransactionFilter.value = filter
    }

    /**
     * Fetches detailed Income and Expense data for the specifically selected month/year.
     * This data feeds the Pie Chart and the Detailed Transaction List.
     * Uses real-time listeners.
     */
    private fun fetchMonthlyData() {
        val currentMonth = _selectedMonth.value
        val currentYear = _selectedYear.value

        // Fetch Monthly Incomes
        firestoreManager.getMonthlyIncomesRealtime(
            month = currentMonth,
            year = currentYear,
            onIncomesLoaded = { incomes -> _monthlyIncomes.value = incomes },
            onFailure = { e -> println("Error fetching monthly incomes (realtime): ${e.message}") }
        )

        // Fetch Monthly Expenses
        firestoreManager.getMonthlyExpensesRealtime(
            month = currentMonth,
            year = currentYear,
            onExpensesLoaded = { expenses -> _monthlyExpenses.value = expenses },
            onFailure = { e -> println("Error fetching monthly expenses (realtime): ${e.message}") }
        )
    }

    /**
     * Fetches aggregated totals for the selected month AND the two preceding months.
     * This data feeds the Bar Chart for trend analysis.
     * Uses one-time fetch logic (Suspend functions/CompletableDeferred).
     */
    private fun fetchChartDataForMonths() {
        viewModelScope.launch {
            val currentMonth = _selectedMonth.value
            val currentYear = _selectedYear.value

            // Prepare list of (Month, Year) pairs to fetch: [Month-2, Month-1, CurrentMonth]
            val monthsToFetch = mutableListOf<Pair<Int, Int>>()
            val calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, currentMonth - 1)
                set(Calendar.YEAR, currentYear)
            }

            // 1. Current Month
            monthsToFetch.add(Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

            // 2. Previous Month
            calendar.add(Calendar.MONTH, -1)
            monthsToFetch.add(0, Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

            // 3. Two Months Ago
            calendar.add(Calendar.MONTH, -1)
            monthsToFetch.add(0, Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

            // Execute fetches in parallel
            val deferredSummaries = monthsToFetch.map { (month, year) ->
                async {
                    var totalIncome = 0.0
                    var totalExpense = 0.0

                    // Wrapper to use callback-based Firestore methods in coroutines
                    val incomesDeferred = CompletableDeferred<List<Income>>()
                    firestoreManager.getMonthlyIncomesOnce(month, year,
                        onSuccess = { incomesDeferred.complete(it) },
                        onFailure = { incomesDeferred.completeExceptionally(it) }
                    )

                    val expensesDeferred = CompletableDeferred<List<Expense>>()
                    firestoreManager.getMonthlyExpensesOnce(month, year,
                        onSuccess = { expensesDeferred.complete(it) },
                        onFailure = { expensesDeferred.completeExceptionally(it) }
                    )

                    try {
                        // Wait for both to complete and calculate sums
                        val incomes = incomesDeferred.await()
                        val expenses = expensesDeferred.await()
                        totalIncome = incomes.sumOf { it.amount }
                        totalExpense = expenses.sumOf { it.amount }
                    } catch (e: Exception) {
                        println("Error fetching summary for $month/$year: ${e.message}")
                    }

                    MonthlySummary(month, year, totalIncome, totalExpense)
                }
            }

            // Wait for all months to process and update state
            val summaries = deferredSummaries.awaitAll()
            // Ensure they are sorted chronologically
            _monthlySummaries.value = summaries.sortedWith(compareBy({ it.year }, { it.month }))
        }
    }

    /**
     * Groups transactions by category and calculates the total amount for each.
     * Used by the Pie Chart.
     *
     * @param transactions A mixed list of Income and Expense objects.
     * @return A Map where Key = Category Name, Value = Total Amount.
     */
    fun getCategorizedTotalsForPieChart(transactions: List<Any>): Map<String, Double> {
        val categorizedTotals = mutableMapOf<String, Double>()

        transactions.forEach { transaction ->
            val category = when (transaction) {
                is Income -> transaction.category
                is Expense -> transaction.category
                else -> "Unknown"
            }

            val amount = when (transaction) {
                is Income -> transaction.amount
                is Expense -> transaction.amount
                else -> 0.0
            }

            categorizedTotals[category] = (categorizedTotals[category] ?: 0.0) + amount
        }

        return categorizedTotals
    }
}