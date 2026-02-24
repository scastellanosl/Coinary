package com.example.coinary.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.view.ChatMessage
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class PredictionUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            "Hola, soy tu predictor financiero. Estoy analizando tus datos históricos para hacer proyecciones precisas. ¿Qué aspecto deseas que analice?",
            false
        )
    ),
    val errorMessage: String? = null
)

private data class MonthFinance(
    val yearMonth: YearMonth,
    val incomes: List<Income>,
    val expenses: List<Expense>
)

@RequiresApi(Build.VERSION_CODES.O)
class PredictionViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    private val GEMINI_API_KEY = "TU_API_KEY"

    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState

    private lateinit var generativeModel: GenerativeModel
    private lateinit var chat: Chat

    // Cache: últimos 6 meses cargados
    private val monthCache = LinkedHashMap<YearMonth, MonthFinance>()
    private var defaultContext6Months: String = ""

    init {
        initializeGemini()
        loadLastSixMonths()
    }

    private fun initializeGemini() {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY
        )

        val initialPrompt = """
            Eres un predictor financiero especializado en proyecciones basadas en patrones históricos.
            Respuestas claras y sin formato (sin negritas, asteriscos o markdown).

            Reglas:
            1. Solo responde sobre proyecciones financieras
            2. Usa exclusivamente los datos proporcionados
            3. Si faltan datos, indícalo claramente
            4. No uses ningún formato especial (nada de **texto** o similares)
            5. Sé específico con cifras cuando sea posible
            6. Proyección basada en tendencias históricas
            7. Factores clave que afectarán el resultado
            8. Recomendaciones para mejorar la proyección
        """.trimIndent()

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(initialPrompt) },
                content(role = "model") {
                    text("Entendido. Operaré como predictor financiero con respuestas claras y sin formato especial.")
                }
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLastSixMonths() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val now = YearMonth.now()
                val monthsToLoad = (0 until 6).map { now.minusMonths(it.toLong()) }.reversed()

                monthCache.clear()

                for (ym in monthsToLoad) {
                    val incomesDeferred = async { getIncomes(ym.monthValue, ym.year) }
                    val expensesDeferred = async { getExpenses(ym.monthValue, ym.year) }

                    val mf = MonthFinance(
                        yearMonth = ym,
                        incomes = incomesDeferred.await(),
                        expenses = expensesDeferred.await()
                    )
                    monthCache[ym] = mf
                }

                defaultContext6Months = buildFinancialContext(monthCache.values.toList())

                _uiState.value = _uiState.value.copy(
                    messages = listOf(
                        ChatMessage(
                            "Listo. Puedes pedirme proyecciones usando: 'este mes', 'mes pasado', un mes (ej: 'febrero') o 'últimos 3 meses'. Ej: '¿Cuánto podré ahorrar en los próximos 3 meses?'",
                            false
                        )
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error cargando datos. Verifica tu conexión e intenta nuevamente.",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun getIncomes(month: Int, year: Int): List<Income> {
        return suspendCoroutine { continuation ->
            firestoreManager.getMonthlyIncomesOnce(
                month, year,
                { incomes -> continuation.resume(incomes) },
                { error -> continuation.resumeWithException(error) }
            )
        }
    }

    private suspend fun getExpenses(month: Int, year: Int): List<Expense> {
        return suspendCoroutine { continuation ->
            firestoreManager.getMonthlyExpensesOnce(
                month, year,
                { expenses -> continuation.resume(expenses) },
                { error -> continuation.resumeWithException(error) }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendMessage(message: String) {
        if (message.isBlank() || _uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(message, true),
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val requestedMonths = parseRequestedMonths(message)
                val selectedMonths = if (requestedMonths.isNullOrEmpty()) {
                    monthCache.values.toList()
                } else {
                    requestedMonths.mapNotNull { monthCache[it] }
                }

                val usedContext = if (selectedMonths.isEmpty()) defaultContext6Months else buildFinancialContext(selectedMonths)
                val periodLabel = buildPeriodLabel(selectedMonths, requestedMonths)

                val prompt = """
                    $periodLabel

                    Datos históricos del usuario (solo el período indicado):
                    $usedContext

                    Consulta de predicción: "$message"

                    Como predictor financiero, proporciona:
                    1. Proyección basada en tendencias históricas del período
                    2. Factores clave que afectarán el resultado
                    3. Recomendaciones para mejorar la proyección
                """.trimIndent()

                val response = chat.sendMessage(prompt)
                val clean = response.text
                    ?.replace("**", "")
                    ?.replace("*", "•")
                    ?: "No pude generar una proyección. Intenta con otra consulta."

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(clean, false),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPeriodLabel(
        selectedMonths: List<MonthFinance>,
        requestedMonths: List<YearMonth>?
    ): String {
        if (requestedMonths.isNullOrEmpty()) {
            val first = monthCache.keys.firstOrNull()
            val last = monthCache.keys.lastOrNull()
            return if (first != null && last != null) {
                "Período analizado (por defecto): ${formatYm(first)} a ${formatYm(last)}"
            } else "Período analizado: últimos meses disponibles"
        }

        if (selectedMonths.isEmpty()) {
            val first = monthCache.keys.firstOrNull()
            val last = monthCache.keys.lastOrNull()
            return if (first != null && last != null) {
                "Nota: pediste un mes fuera del rango cargado. Usaré ${formatYm(first)} a ${formatYm(last)}"
            } else "Nota: pediste un mes fuera del rango cargado. Usaré últimos meses disponibles"
        }

        return if (selectedMonths.size == 1) {
            "Período analizado: ${formatYm(selectedMonths.first().yearMonth)}"
        } else {
            "Período analizado: ${formatYm(selectedMonths.first().yearMonth)} a ${formatYm(selectedMonths.last().yearMonth)}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatYm(ym: YearMonth): String {
        val monthName = ym.month.getDisplayName(TextStyle.FULL, Locale("es", "CO"))
        return "${monthName.replaceFirstChar { it.titlecase(Locale("es", "CO")) }} ${ym.year}"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildFinancialContext(months: List<MonthFinance>): String {
        val sb = StringBuilder()
        sb.appendLine("Datos Financieros Históricos del Usuario:")

        months.forEach { mf ->
            val monthName = mf.yearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "CO"))
            val year = mf.yearMonth.year

            val totalIncome = mf.incomes.sumOf { it.amount }
            val totalExpense = mf.expenses.sumOf { it.amount }
            val savings = totalIncome - totalExpense

            sb.appendLine()
            sb.appendLine("Mes: ${monthName.replaceFirstChar { it.titlecase(Locale("es", "CO")) }} $year")
            sb.appendLine("- Ingresos totales: ${"%.2f".format(totalIncome)}")
            sb.appendLine("- Gastos totales: ${"%.2f".format(totalExpense)}")
            sb.appendLine("- Ahorro: ${"%.2f".format(savings)}")

            if (mf.expenses.isNotEmpty()) {
                val topCategories = mf.expenses.groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }
                    .take(3)

                sb.appendLine("- Top 3 categorías de gasto:")
                topCategories.forEach { (category, amount) ->
                    sb.appendLine("  • $category: ${"%.2f".format(amount)}")
                }
            }
        }

        // Tendencias dentro del período seleccionado
        val monthlySavings = months.map { it.incomes.sumOf { i -> i.amount } - it.expenses.sumOf { e -> e.amount } }
        if (monthlySavings.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Tendencias:")
            sb.appendLine("- Ahorro mensual promedio: ${"%.2f".format(monthlySavings.average())}")
            sb.appendLine("- Mejor mes de ahorro: ${"%.2f".format(monthlySavings.maxOrNull() ?: 0.0)}")
            sb.appendLine("- Peor mes de ahorro: ${"%.2f".format(monthlySavings.minOrNull() ?: 0.0)}")
        }

        return sb.toString()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseRequestedMonths(message: String): List<YearMonth>? {
        val now = YearMonth.now()
        val m = normalizeEs(message)

        if (Regex("\\b(este\\s+mes|mes\\s+actual)\\b").containsMatchIn(m)) {
            return listOf(now)
        }

        if (Regex("\\b(mes\\s+(pasado|anterior|previo)|ultimo\\s+mes)\\b").containsMatchIn(m)) {
            return listOf(now.minusMonths(1))
        }

        Regex("\\bultimos?\\s+(\\d{1,2})\\s+meses\\b").find(m)?.let { match ->
            val n = match.groupValues[1].toIntOrNull() ?: return null
            val safeN = n.coerceIn(1, 24)
            return (safeN - 1 downTo 0).map { now.minusMonths(it.toLong()) }
        }

        val monthRegex =
            Regex("\\b(ene|enero|feb|febrero|mar|marzo|abr|abril|may|mayo|jun|junio|jul|julio|ago|agosto|sep|septiembre|setiembre|oct|octubre|nov|noviembre|dic|diciembre)\\b(?:\\s*(?:de)?\\s*(\\d{4}))?")

        val found = monthRegex.find(m) ?: return null
        val monthToken = found.groupValues[1]
        val yearToken = found.groupValues.getOrNull(2).orEmpty()

        val monthValue = monthTokenToNumber(monthToken) ?: return null
        val yearValue = yearToken.toIntOrNull() ?: inferYear(now, monthValue)

        return listOf(YearMonth.of(yearValue, monthValue))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun inferYear(now: YearMonth, monthValue: Int): Int {
        return if (monthValue > now.monthValue) now.year - 1 else now.year
    }

    private fun monthTokenToNumber(token: String): Int? {
        return when (token) {
            "ene", "enero" -> 1
            "feb", "febrero" -> 2
            "mar", "marzo" -> 3
            "abr", "abril" -> 4
            "may", "mayo" -> 5
            "jun", "junio" -> 6
            "jul", "julio" -> 7
            "ago", "agosto" -> 8
            "sep", "septiembre", "setiembre" -> 9
            "oct", "octubre" -> 10
            "nov", "noviembre" -> 11
            "dic", "diciembre" -> 12
            else -> null
        }
    }

    private fun normalizeEs(input: String): String {
        val lower = input.lowercase(Locale("es", "CO"))
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return normalized.replace("\\p{Mn}+".toRegex(), "")
    }

    fun resetErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

