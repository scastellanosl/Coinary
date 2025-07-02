package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred // Importar CompletableDeferred
import java.util.Calendar
import java.util.Date

// Enum para los tipos de gráfica
enum class ChartType {
    BAR, LINE, PIE
}

// Enum para los tipos de filtro de transacción
enum class TransactionFilter {
    ALL, INCOME, EXPENSE
}

// Data class para el resumen mensual (para la gráfica de barras)
data class MonthlySummary(
    val month: Int, // 1-12
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double
)

class StatsViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    // Estado para el mes y año seleccionados
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    // Estado para el tipo de gráfica seleccionada
    private val _selectedChartType = MutableStateFlow(ChartType.BAR)
    val selectedChartType: StateFlow<ChartType> = _selectedChartType

    // Estado para el tipo de filtro de transacción
    private val _selectedTransactionFilter = MutableStateFlow(TransactionFilter.ALL)
    val selectedTransactionFilter: StateFlow<TransactionFilter> = _selectedTransactionFilter

    // Datos para las gráficas (ingresos y gastos mensuales del mes seleccionado)
    private val _monthlyIncomes = MutableStateFlow<List<Income>>(emptyList())
    val monthlyIncomes: StateFlow<List<Income>> = _monthlyIncomes

    private val _monthlyExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val monthlyExpenses: StateFlow<List<Expense>> = _monthlyExpenses

    // Nuevo StateFlow para los resúmenes mensuales (para la gráfica de barras multi-mes)
    private val _monthlySummaries = MutableStateFlow<List<MonthlySummary>>(emptyList())
    val monthlySummaries: StateFlow<List<MonthlySummary>> = _monthlySummaries

    // Transacciones filtradas para el registro de abajo (combinando ingresos y gastos)
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
                    else -> Date(0) // Fallback date
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
        // Iniciar la carga de datos cuando el ViewModel se crea
        fetchMonthlyData() // Carga datos para el mes seleccionado (real-time)
        fetchChartDataForMonths() // Carga datos para la gráfica de barras multi-mes (one-time)
    }

    /**
     * Actualiza el mes y año seleccionados y recarga los datos.
     * Esto activará tanto las cargas de datos de un solo mes como de varios meses.
     */
    fun updateSelectedDate(month: Int, year: Int) {
        _selectedMonth.value = month
        _selectedYear.value = year
        fetchMonthlyData() // Recargar datos de un solo mes
        fetchChartDataForMonths() // Recargar datos de varios meses para la gráfica de barras
    }

    /**
     * Actualiza el tipo de gráfica seleccionada.
     */
    fun updateChartType(chartType: ChartType) {
        _selectedChartType.value = chartType
    }

    /**
     * Actualiza el tipo de filtro para las transacciones.
     */
    fun updateTransactionFilter(filter: TransactionFilter) {
        _selectedTransactionFilter.value = filter
    }

    /**
     * Carga los datos de ingresos y gastos para el mes y año seleccionados (real-time).
     * Estos datos se usan para las gráficas de líneas/torta y la lista de transacciones.
     */
    private fun fetchMonthlyData() {
        viewModelScope.launch {
            val currentMonth = _selectedMonth.value
            val currentYear = _selectedYear.value

            // Cargar ingresos mensuales en tiempo real
            firestoreManager.getMonthlyIncomesRealtime(
                month = currentMonth,
                year = currentYear,
                onIncomesLoaded = { incomes -> _monthlyIncomes.value = incomes },
                onFailure = { e -> println("Error fetching monthly incomes (realtime): ${e.message}") }
            )

            // Cargar gastos mensuales en tiempo real
            firestoreManager.getMonthlyExpensesRealtime(
                month = currentMonth,
                year = currentYear,
                onExpensesLoaded = { expenses -> _monthlyExpenses.value = expenses },
                onFailure = { e -> println("Error fetching monthly expenses (realtime): ${e.message}") }
            )
        }
    }

    /**
     * Carga los datos de ingresos y gastos para la gráfica de barras multi-mes (one-time fetch).
     * Obtiene datos para el mes seleccionado y los dos meses anteriores.
     */
    private fun fetchChartDataForMonths() {
        viewModelScope.launch {
            val currentMonth = _selectedMonth.value
            val currentYear = _selectedYear.value

            val monthsToFetch = mutableListOf<Pair<Int, Int>>() // Par: mes, año

            // Calcular los tres meses a obtener (actual y dos anteriores)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, currentMonth - 1)
                set(Calendar.YEAR, currentYear)
            }

            // Añadir mes actual
            monthsToFetch.add(Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

            // Añadir mes anterior
            calendar.add(Calendar.MONTH, -1)
            monthsToFetch.add(0, Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

            // Añadir dos meses atrás
            calendar.add(Calendar.MONTH, -1)
            monthsToFetch.add(0, Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR)))

            val deferredSummaries = monthsToFetch.map { (month, year) ->
                async {
                    var totalIncome = 0.0
                    var totalExpense = 0.0

                    // Usando CompletableDeferred para convertir las callbacks de Firestore en suspend functions
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
                        totalIncome = incomesDeferred.await().sumOf { it.amount }
                        totalExpense = expensesDeferred.await().sumOf { it.amount }
                    } catch (e: Exception) {
                        println("Error fetching data for month $month/$year: ${e.message}")
                    }
                    MonthlySummary(month, year, totalIncome, totalExpense)
                }
            }
            // Esperar a que todas las cargas se completen y ordenar por año y mes
            _monthlySummaries.value = deferredSummaries.awaitAll().sortedWith(compareBy({ it.year }, { it.month }))
        }
    }

    /**
     * Prepara los datos para la gráfica de barras o líneas (total por día del mes).
     * @return Un mapa donde la clave es el día del mes y el valor es el monto total.
     */
    fun getDailyTotalsForChart(transactions: List<Any>): Map<Int, Double> {
        val dailyTotals = mutableMapOf<Int, Double>()
        transactions.forEach { transaction ->
            val date = when (transaction) {
                is Income -> transaction.date.toDate()
                is Expense -> transaction.date.toDate()
                else -> null
            }
            date?.let {
                val calendar = Calendar.getInstance().apply { time = it }
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                dailyTotals[dayOfMonth] = (dailyTotals[dayOfMonth] ?: 0.0) +
                        when (transaction) {
                            is Income -> transaction.amount
                            is Expense -> transaction.amount
                            else -> 0.0
                        }
            }
        }
        return dailyTotals.toSortedMap() // Devuelve un mapa ordenado por día
    }

    /**
     * Prepara los datos para la gráfica de pastel (total por categoría).
     * @return Un mapa donde la clave es la categoría y el valor es el monto total.
     */
    fun getCategorizedTotalsForPieChart(transactions: List<Any>): Map<String, Double> {
        val categorizedTotals = mutableMapOf<String, Double>()
        transactions.forEach { transaction ->
            val category = when (transaction) {
                is Income -> transaction.category
                is Expense -> transaction.category
                else -> "Desconocido"
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