package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * Enum representing the available time ranges for filtering data.
 */
enum class TimeRange { DAY, WEEK, MONTH, YEAR }

/**
 * ViewModel for the Home Screen.
 * Handles the retrieval of total balances and manages the logic for filtering
 * categorized expenses based on selected time ranges.
 */
class HomeViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    // --- Dynamic Totals State (Filtered by TimeRange) ---
    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome

    private val _totalExpenses = MutableStateFlow(0.0)
    val totalExpenses: StateFlow<Double> = _totalExpenses

    // --- Filter State ---
    // Tracks the currently selected time filter (defaults to WEEK)
    private val _selectedTimeRange = MutableStateFlow(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange

    // --- Filtered Data State ---
    // Holds the expense data grouped by category, updated dynamically based on the selected TimeRange.
    private val _weeklyCategorizedExpenses = MutableStateFlow<Map<String, Double>>(emptyMap())
    val weeklyCategorizedExpenses: StateFlow<Map<String, Double>> = _weeklyCategorizedExpenses

    init {
        // Initialize with the default time range (Week)
        setTimeRange(TimeRange.WEEK)
    }

    /**
     * Updates the selected time range and triggers a data fetch for the new period.
     * Intended to be called from the UI layer.
     *
     * This now updates 3 things:
     * 1. The Pie Chart data (Categorized Expenses)
     * 2. The Total Income Text
     * 3. The Total Expense Text
     *
     * @param range The new TimeRange selected by the user.
     */
    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
        fetchDataForRange(range)
    }

    /**
     * Calculates the date bounds and requests filtered data from Firestore.
     */
    private fun fetchDataForRange(range: TimeRange) {
        // 1. Calculate the start and end dates based on the selected filter
        val (startDate, endDate) = calculateDateRange(range)
        val startTs = Timestamp(startDate)
        val endTs = Timestamp(endDate)

        viewModelScope.launch {
            // 2. Fetch Categorized Expenses for the Pie Chart
            firestoreManager.getExpensesByCategoryByDateRangeRealtime(
                startDate = startTs,
                endDate = endTs,
                onCategorizedExpensesLoaded = { categorizedData ->
                    _weeklyCategorizedExpenses.value = categorizedData
                },
                onFailure = { e ->
                    println("Error fetching filtered expenses chart: ${e.message}")
                }
            )

            // 3. Fetch Total Income for the selected range
            firestoreManager.getIncomeSumByDateRangeRealtime(
                startDate = startTs,
                endDate = endTs,
                onResult = { total ->
                    _totalIncome.value = total
                },
                onFailure = { e ->
                    println("Error fetching filtered total income: ${e.message}")
                }
            )

            // 4. Fetch Total Expenses for the selected range
            firestoreManager.getExpenseSumByDateRangeRealtime(
                startDate = startTs,
                endDate = endTs,
                onResult = { total ->
                    _totalExpenses.value = total
                },
                onFailure = { e ->
                    println("Error fetching filtered total expenses: ${e.message}")
                }
            )
        }
    }

    /**
     * Helper function to calculate the Start and End Date objects based on the provided TimeRange.
     * @return A Pair containing (StartDate, EndDate).
     */
    private fun calculateDateRange(range: TimeRange): Pair<Date, Date> {
        val calendar = Calendar.getInstance()

        // Configure the end of the range to be the very end of the current day (23:59:59.999)
        val endCalendar = Calendar.getInstance()
        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)
        endCalendar.set(Calendar.MILLISECOND, 999)

        // Configure the start date initialization (00:00:00.000)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (range) {
            TimeRange.DAY -> {
                // Start: Today at 00:00
                // End: Today at 23:59
                // No additional manipulation needed for 'calendar' as it is already set to today.
            }
            TimeRange.WEEK -> {
                // Adjust to the first day of the week (Monday)
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            TimeRange.MONTH -> {
                // Adjust to the first day of the current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            }
            TimeRange.YEAR -> {
                // Adjust to the first day of the current year (January 1st)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return Pair(calendar.time, endCalendar.time)
    }
}