package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.view.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.type.content
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

data class PredictionUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hola, soy tu predictor financiero. Estoy analizando tus datos históricos para hacer proyecciones precisas. ¿Qué aspecto deseas que analice?", false)
    ),
    val errorMessage: String? = null
)

class PredictionViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {
    private val GEMINI_API_KEY = ""
    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState

    private lateinit var generativeModel: GenerativeModel
    private lateinit var chat: Chat
    private var userFinancialData: String = ""
    private val scope = MainScope()

    init {
        initializeGemini()
        loadUserFinancialData()
    }

    private fun initializeGemini() {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = ""
        )

        val initialPrompt = """
            Eres un predictor financiero especializado en proyecciones basadas en patrones históricos. 
            Respuestas claras y sin formato (sin negritas, asteriscos o markdown).
            
            Reglas:
            1. Solo responde sobre proyecciones financieras
            2. Usa exclusivamente los datos proporcionados y razona para entender la jerga o habla de las personas
            3. Si faltan datos, indícalo claramente
            4. No uses ningún formato especial (nada de **texto** o similares)
            5. Sé específico con cifras cuando sea posible
            6. Proyección basada en tendencias históricas
            7. Factores clave que afectarán el resultado
            8. Recomendaciones para mejorar la proyección
            
            Cuando recibas la consulta, analizaré los datos históricos del usuario.
        """.trimIndent()

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(initialPrompt) },
                content(role = "model") {
                    text("Entendido. Operaré como predictor financiero con respuestas claras y sin formato especial. Por favor, proporcióname tu consulta después de que cargue tus datos históricos.")
                }
            )
        )
    }

    private fun loadUserFinancialData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        scope.launch {
            try {
                // Obtener datos de los últimos meses para mejor análisis
                val financialData = mutableListOf<Pair<List<Income>, List<Expense>>>()
                val calendar = Calendar.getInstance()

                repeat(6) { i ->
                    calendar.add(Calendar.MONTH, if (i == 0) 0 else -1)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val year = calendar.get(Calendar.YEAR)

                    val incomes = async { getIncomes(month, year) }
                    val expenses = async { getExpenses(month, year) }

                    financialData.add(Pair(incomes.await(), expenses.await()))
                }

                userFinancialData = buildFinancialContext(financialData.reversed()) // Ordenar de más antiguo a más reciente

                _uiState.value = _uiState.value.copy(
                    messages = listOf(
                        ChatMessage("He analizado tus últimos meses. ¿Qué proyección financiera deseas? Por ejemplo: '¿Cuánto podré ahorrar en los próximos 3 meses?'", false)
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error cargando datos. Por favor, verifica tu conexión e intenta nuevamente.",
                    isLoading = false
                )
                userFinancialData = "Error: ${e.localizedMessage}"
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

    private fun buildFinancialContext(historicalData: List<Pair<List<Income>, List<Expense>>>): String {
        val sb = StringBuilder()
        sb.appendln("Datos Financieros Históricos del Usuario:")

        historicalData.forEachIndexed { index, (incomes, expenses) ->
            val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, - (historicalData.size - 1 - index)) }
            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())!!
            val year = calendar.get(Calendar.YEAR)

            val totalIncome = incomes.sumOf { it.amount }
            val totalExpense = expenses.sumOf { it.amount }
            val savings = totalIncome - totalExpense

            sb.appendln("\nMes: $monthName $year")
            sb.appendln("- Ingresos totales: ${"%.2f".format(totalIncome)}")
            sb.appendln("- Gastos totales: ${"%.2f".format(totalExpense)}")
            sb.appendln("- Ahorro: ${"%.2f".format(savings)}")

            if (expenses.isNotEmpty()) {
                val topCategories = expenses.groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }
                    .take(3)

                sb.appendln("- Top 3 categorías de gasto:")
                topCategories.forEach { (category, amount) ->
                    sb.appendln("  • $category: ${"%.2f".format(amount)}")
                }
            }
        }

        // Análisis de tendencias
        sb.appendln("\nTendencias:")
        val monthlySavings = historicalData.map { it.first.sumOf { it.amount } - it.second.sumOf { it.amount } }
        sb.appendln("- Ahorro mensual promedio: ${"%.2f".format(monthlySavings.average())}")
        sb.appendln("- Mejor mes de ahorro: ${"%.2f".format(monthlySavings.maxOrNull() ?: 0.0)}")
        sb.appendln("- Peor mes de ahorro: ${"%.2f".format(monthlySavings.minOrNull() ?: 0.0)}")

        return sb.toString()
    }

    fun sendMessage(message: String) {
        if (message.isBlank()) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(message, true),
            isLoading = true,
            errorMessage = null
        )

        scope.launch {
            try {
                val response = generateResponse(message)

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(response, false),
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

    private suspend fun generateResponse(userMessage: String): String {
        return try {
            val prompt = """
                Datos históricos del usuario (últimos 3 meses):
                $userFinancialData
                
                Consulta de predicción: "$userMessage"
                
                Como predictor financiero, proporciona:
                1. Proyección basada en tendencias históricas
                2. Factores clave que afectarán el resultado
                3. Recomendaciones para mejorar la proyección
            """.trimIndent()

            val response = chat.sendMessage(prompt)
            response.text ?: "No pude generar una proyección. Por favor, intenta con otra consulta."
        } catch (e: Exception) {
            "Error técnico: ${e.localizedMessage}. Por favor, inténtalo de nuevo más tarde."
        }
    }

    fun resetErrorMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}
