package com.example.coinary.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Enum representing the available time ranges for filtering dashboard data.
 */
enum class TimeRange {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

/**
 * HomeViewModel: Manages the state for the Home Dashboard.
 * Handles the retrieval of total balances (Income vs Expense) and categorized data
 * for the Pie Chart, filtering everything based on the selected [TimeRange].
 *
 * @property firestoreManager The repository class for Firestore operations.
 */
class HomeViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    // --- State: Dynamic Totals (Filtered by TimeRange) ---
    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome.asStateFlow()

    private val _totalExpenses = MutableStateFlow(0.0)
    val totalExpenses: StateFlow<Double> = _totalExpenses.asStateFlow()

    // --- State: Filter Selection ---
    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()

    // --- State: Filtered Categorized Data ---
    private val _categorizedExpenses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val categorizedExpenses: StateFlow<Map<String, Double>> = _categorizedExpenses.asStateFlow()

    init {
        // Initialize dashboard with the default time range (Week-to-date)
        setTimeRange(TimeRange.WEEK)
    }

    /**
     * Updates the selected time range and triggers a multi-source data fetch.
     * This refresh updates:
     * 1. Categorized Expenses (for Pie Charts).
     * 2. Total Income sum.
     * 3. Total Expense sum.
     *
     * @param range The new [TimeRange] selected by the user.
     */
    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        fetchDataForRange(range)
    }

    /**
     * Calculates date boundaries and launches parallel coroutines to fetch filtered data.
     *
     * @param range Selected period to calculate the (Start, End) date pair.
     */
    private fun fetchDataForRange(range: TimeRange) {
        val (startDate, endDate) = calculateDateRange(range)
        val startTs = Timestamp(startDate)
        val endTs = Timestamp(endDate)

        viewModelScope.launch {
            // Fetch Grouped Expenses by Category
            firestoreManager.getExpensesByCategoryByDateRangeRealtime(
                startDate = startTs,
                endDate = endTs,
                onCategorizedExpensesLoaded = { categorizedData ->
                    _categorizedExpenses.value = categorizedData
                },
                onFailure = { e ->
                    Log.e("HomeViewModel", "Error fetching categorized data: ${e.message}")
                }
            )

            // Fetch Total Income Sum
            firestoreManager.getIncomeSumByDateRangeRealtime(
                startDate = startTs,
                endDate = endTs,
                onResult = { total ->
                    _totalIncome.value = total
                },
                onFailure = { e ->
                    Log.e("HomeViewModel", "Error fetching total income: ${e.message}")
                }
            )

            // Fetch Total Expense Sum
            firestoreManager.getExpenseSumByDateRangeRealtime(
                startDate = startTs,
                endDate = endTs,
                onResult = { total ->
                    _totalExpenses.value = total
                },
                onFailure = { e ->
                    Log.e("HomeViewModel", "Error fetching total expenses: ${e.message}")
                }
            )
        }
    }

    /**
     * Helper to determine the precise start and end of a time period.
     * - Start: 00:00:00.000 of the calculated first day.
     * - End: 23:59:59.999 of the current day.
     *
     * @return A [Pair] containing the Start [Date] and End [Date].
     */
    private fun calculateDateRange(range: TimeRange): Pair<Date, Date> {
        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (range) {
            TimeRange.DAY -> { /* Uses current day by default */ }
            TimeRange.WEEK -> {
                startCalendar.firstDayOfWeek = Calendar.MONDAY
                startCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            TimeRange.MONTH -> {
                startCalendar.set(Calendar.DAY_OF_MONTH, 1)
            }
            TimeRange.YEAR -> {
                startCalendar.set(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return Pair(startCalendar.time, endCalendar.time)
    }
}