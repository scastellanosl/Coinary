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
import kotlin.math.roundToInt

data class PredictionUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hola, soy tu predictor financiero. Analizaré tus patrones de gastos para proyectar tu futuro financiero. ¿Qué deseas predecir?", false)
    ),
    val errorMessage: String? = null
)

class PredictionViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {
    private val GEMINI_API_KEY = "AIzaSyBmPWQsscLDqGl-vsV38VKrWZvhkexu7z0"
    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState

    private lateinit var generativeModel: GenerativeModel
    private lateinit var chat: Chat
    private var userFinancialData: String = "Cargando datos financieros..."
    private val scope = MainScope()

    init {
        loadUserFinancialData()
        initializeGemini()
    }

    private fun initializeGemini() {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY
        )

        val initialPrompt = """
            Eres un predictor financiero especializado en proyecciones basadas en patrones históricos. 
            Solo responderás preguntas relacionadas con predicciones financieras.
            
            Reglas estrictas:
            1. Solo haz proyecciones financieras
            2. Basa tus predicciones en los datos proporcionados
            3. Proporciona rangos probables (optimista, pesimista y realista)
            4. Considera tendencias históricas
            5. Usa un tono profesional pero claro
            6. Si no hay suficientes datos, indícalo claramente
            
            Cuando el usuario haga una consulta, recibirás sus datos financieros históricos.
        """.trimIndent()

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(initialPrompt) },
                content(role = "model") {
                    text("Listo como tu predictor financiero. Estoy analizando tus datos históricos. ¿Qué aspecto deseas proyectar? (ahorros futuros, deudas potenciales, crecimiento patrimonial, etc.)")
                }
            )
        )
    }

    private fun loadUserFinancialData() {
        scope.launch {
            try {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)

                val financialData = mutableListOf<Pair<List<Income>, List<Expense>>>()

                for (i in 0..2) {
                    calendar.add(Calendar.MONTH, -1)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val year = calendar.get(Calendar.YEAR)
                    financialData.add(getMonthlyFinancialData(month, year))
                }

                userFinancialData = buildFinancialContext(financialData)

                _uiState.value = _uiState.value.copy(
                    messages = listOf(
                        ChatMessage("He analizado tus últimos 3 meses. ¿Qué proyección financiera deseas?", false)
                    )
                )
            } catch (e: Exception) {
                userFinancialData = "Error al cargar datos: ${e.localizedMessage}"
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudieron cargar los datos históricos"
                )
            }
        }
    }

    private suspend fun getMonthlyFinancialData(month: Int, year: Int): Pair<List<Income>, List<Expense>> {
        val incomes = mutableListOf<Income>()
        val expenses = mutableListOf<Expense>()

        val incomesResult = firestoreManager.getMonthlyIncomesOnce(month, year,
            { incomesList -> incomes.addAll(incomesList) },
            { throw it }
        )

        val expensesResult = firestoreManager.getMonthlyExpensesOnce(month, year,
            { expensesList -> expenses.addAll(expensesList) },
            { throw it }
        )

        return Pair(incomes, expenses)
    }

    private fun buildFinancialContext(historicalData: List<Pair<List<Income>, List<Expense>>>): String {
        val sb = StringBuilder()
        sb.appendln("|Datos Históricos para Predicciones:")

        historicalData.forEachIndexed { index, (incomes, expenses) ->
            val monthOffset = historicalData.size - index - 1
            val calendar = Calendar.getInstance().apply { add(Calendar.MONTH, -monthOffset) }
            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

            sb.appendln("|Mes: $monthName")
            sb.appendln("|Ingresos totales: ${incomes.sumOf { it.amount }}")
            sb.appendln("|Gastos totales: ${expenses.sumOf { it.amount }}")
            sb.appendln("|Ahorro: ${incomes.sumOf { it.amount } - expenses.sumOf { it.amount }}")

            val topExpenses = expenses.groupBy { it.category }
                .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
                .entries.sortedByDescending { it.value }
                .take(3)

            sb.appendln("|Top 3 gastos: ${topExpenses.joinToString { "${it.key} (${it.value})" }}")
            sb.appendln("|-------------------------")
        }

        val avgSavings = historicalData.map { it.first.sumOf { it.amount } - it.second.sumOf { it.amount } }.average()
        val avgIncome = historicalData.map { it.first.sumOf { it.amount } }.average()
        val avgExpense = historicalData.map { it.second.sumOf { it.amount } }.average()

        sb.appendln("|Tendencias:")
        sb.appendln("|Ahorro promedio mensual: $avgSavings")
        sb.appendln("|Ingreso promedio mensual: $avgIncome")
        sb.appendln("|Gasto promedio mensual: $avgExpense")

        return sb.toString().trimMargin()
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
                2. 3 escenarios (optimista, pesimista y realista)
                3. Factores clave que afectarán el resultado
                4. Recomendaciones para mejorar la proyección
                5. Máximo 300 palabras
                6. Si faltan datos, indícalo claramente
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