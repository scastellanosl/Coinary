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

data class RecommendationUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            "Hola, soy tu asesor financiero personal. Estoy analizando tus movimientos para darte recomendaciones precisas y personalizadas. Dame un momento mientras cargo tus datos...",
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
class RecommendationViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    private val GEMINI_API_KEY = ""

    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState

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
            Eres un asesor financiero especializado en finanzas personales con estas reglas estrictas:
            1. Dale a conocer al usuario los datos que ha gastado como un balance general
            2. Enfoque: Solo responder sobre gestión financiera personal
            3. Base de datos: Usar exclusivamente los datos proporcionados
            4. Formato:
               - Sin negritas, asteriscos o markdown
               - Estructura clara con viñetas
            5. Recomendaciones:
               - Siempre proporcionar 3-5 recomendaciones accionables
               - Priorizar por impacto potencial
               - Incluir pasos concretos
               - Basar cada sugerencia en datos específicos
            6. Patrones peligrosos:
               - Señalar claramente cualquier patrón riesgoso
               - Explicar consecuencias potenciales
            7. Lenguaje:
               - Claro y directo
               - Evitar jerga financiera compleja
               - Tono empático pero profesional
            Los datos históricos del usuario serán proporcionados a continuación.
        """.trimIndent()

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(initialPrompt) },
                content(role = "model") {
                    text("Entendido. Operaré como asesor financiero personal con respuestas claras, prácticas y basadas en datos.")
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
                            "He analizado tus últimos meses. ¿Quieres recomendaciones de este mes, el mes pasado o un mes específico (ej: 'febrero')?",
                            false
                        )
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error cargando datos. Por favor, verifica tu conexión.",
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

                val usedContext = if (selectedMonths.isEmpty()) {
                    defaultContext6Months
                } else {
                    buildFinancialContext(selectedMonths)
                }

                val periodLabel = buildPeriodLabel(selectedMonths, requestedMonths)

                val prompt = """
                    $periodLabel

                    Datos del usuario (solo el período indicado):
                    $usedContext

                    Consulta específica: "$message"

                    Por favor:
                    1. Analiza los patrones financieros del período
                    2. Proporciona 3-5 recomendaciones accionables
                    3. Señala cualquier patrón riesgoso
                    4. Mantén un lenguaje claro sin formatos especiales
                """.trimIndent()

                val response = chat.sendMessage(prompt)
                val cleanResponse = response.text
                    ?.replace("**", "")
                    ?.replace("*", "•")
                    ?: "No pude generar recomendaciones. Por favor reformula tu pregunta."

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(cleanResponse, false),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al generar recomendaciones. Intenta nuevamente.",
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
            } else {
                "Período analizado: últimos meses disponibles"
            }
        }

        if (selectedMonths.isEmpty()) {
            val first = monthCache.keys.firstOrNull()
            val last = monthCache.keys.lastOrNull()
            return if (first != null && last != null) {
                "Nota: pediste un mes fuera del rango cargado. Usaré ${formatYm(first)} a ${formatYm(last)}"
            } else {
                "Nota: pediste un mes fuera del rango cargado. Usaré últimos meses disponibles"
            }
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

    private fun buildFinancialContext(months: List<MonthFinance>): String {
        val sb = StringBuilder()
        sb.appendLine("Análisis Financiero Detallado:")

        val allIncomes = months.flatMap { it.incomes }
        val allExpenses = months.flatMap { it.expenses }

        val totalIncome = allIncomes.sumOf { it.amount }
        val totalExpense = allExpenses.sumOf { it.amount }

        sb.appendLine()
        sb.appendLine("● Resumen del período:")
        sb.appendLine("- Ingresos totales: ${"%.2f".format(totalIncome)}")
        sb.appendLine("- Gastos totales: ${"%.2f".format(totalExpense)}")
        sb.appendLine("- Balance (ingresos - gastos): ${"%.2f".format(totalIncome - totalExpense)}")

        if (allExpenses.isNotEmpty()) {
            val expenseAnalysis = allExpenses.groupBy { it.category }
                .mapValues { (_, expenses) ->
                    val total = expenses.sumOf { it.amount }
                    val avg = total / months.size.coerceAtLeast(1)
                    total to avg
                }
                .entries
                .sortedByDescending { it.value.first }

            sb.appendLine()
            sb.appendLine("● Distribución de gastos:")
            expenseAnalysis.forEach { entry ->
                val category = entry.key
                val total = entry.value.first
                val monthlyAvg = entry.value.second
                sb.appendLine("- $category: ${"%.2f".format(total)} total (${ "%.2f".format(monthlyAvg) }/mes)")
            }

            sb.appendLine()
            sb.appendLine("● Patrones detectados:")
            val variableExpenses = expenseAnalysis.filter { it.key !in listOf("Vivienda", "Transporte") }
            val highestVariable = variableExpenses.firstOrNull()
            highestVariable?.let {
                sb.appendLine("- Mayor gasto variable: ${it.key} (${ "%.2f".format(it.value.second) }/mes)")
            }
        }

        sb.appendLine()
        sb.appendLine("● Evolución mensual:")
        months.forEach { mf ->
            val monthIncome = mf.incomes.sumOf { it.amount }
            val monthExpense = mf.expenses.sumOf { it.amount }
            val savings = monthIncome - monthExpense
            val pct = if (monthIncome > 0.0) (savings * 100.0 / monthIncome) else 0.0
            sb.appendLine("- ${formatYm(mf.yearMonth)}: ${"%.2f".format(savings)} (${ "%.0f".format(pct) }% de ahorro)")
        }

        return sb.toString()
    }

    // -----------------------------
    // Detección “filtros” por texto
    // -----------------------------
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
            return (safeN - 1 downTo 0).map { now.minusMonths(it.toLong()) } // viejo -> reciente [web:18]
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
        return normalized.replace("\\p{Mn}+".toRegex(), "") // quita tildes: “último” -> “ultimo”
    }

    fun resetErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

