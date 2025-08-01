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
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.Chat
import kotlinx.coroutines.async
import java.util.Calendar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.math.roundToInt

data class RecommendationUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hola, soy tu asesor financiero personal. Analizaré tus movimientos para darte recomendaciones precisas. ¿En qué aspecto financiero necesitas ayuda hoy?", false)
    ),
    val errorMessage: String? = null
)

class RecommendationViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {
    private val GEMINI_API_KEY = "AIzaSyBmPWQsscLDqGl-vsV38VKrWZvhkexu7z0"
    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState

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
            Eres un asesor financiero especializado en gastos hormiga y gestión financiera personal. 
            Solo responderás preguntas relacionadas con finanzas personales, ahorro, inversión y gestión de gastos.
            Recibirás información detallada sobre los movimientos financieros del usuario.
            
            Reglas estrictas:
            1. Solo habla de temas financieros
            2. Basa tus recomendaciones exclusivamente en los datos proporcionados
            3. Sé específico y práctico en tus consejos
            4. Usa un tono profesional pero cercano
            5. Si preguntan sobre otro tema, indica cortésmente que solo puedes ayudar con asuntos financieros
            
            Cuando el usuario haga una consulta, recibirás sus datos financieros actualizados.
        """.trimIndent()

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(initialPrompt) },
                content(role = "model") {
                    text("Listo como tu asesor financiero. Estoy cargando tus datos. ¿En qué área específica necesitas recomendaciones? (presupuesto, reducción de gastos, plan de ahorro, etc.)")
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

                val (incomes, expenses) = getMonthlyFinancialData(currentMonth, currentYear)
                userFinancialData = buildFinancialContext(incomes, expenses)

                // Actualizar el mensaje inicial con datos cargados
                _uiState.value = _uiState.value.copy(
                    messages = listOf(
                        ChatMessage("Hola, he analizado tus movimientos recientes. ¿En qué aspecto financiero necesitas ayuda hoy?", false)
                    )
                )
            } catch (e: Exception) {
                userFinancialData = "Error al cargar datos: ${e.localizedMessage}"
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudieron cargar todos los datos financieros"
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

    private fun buildFinancialContext(incomes: List<Income>, expenses: List<Expense>): String {
        val totalIncome = incomes.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val savings = totalIncome - totalExpenses

        val expensesByCategory = expenses.groupBy { it.category }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        val topCategories = expensesByCategory.take(3).joinToString {
            "${it.key}: ${it.value} (${((it.value/totalExpenses)*100).roundToInt()}%)"
        }

        return """
            |Resumen Financiero del Usuario:
            |
            |Ingresos totales: $totalIncome
            |Gastos totales: $totalExpenses
            |Ahorro neto: $savings
            |
            |Distribución de Gastos:
            |${expensesByCategory.joinToString("\n") { "- ${it.key}: ${it.value}" }}
            |
            |Principales categorías de gasto: $topCategories
            |
            |Últimos movimientos:
            |Ingresos recientes:
            |${incomes.takeLast(3).joinToString("\n") { "- ${it.description}: ${it.amount} (${it.category})" }}
            |
            |Gastos recientes:
            |${expenses.takeLast(3).joinToString("\n") { "- ${it.description}: ${it.amount} (${it.category})" }}
        """.trimMargin()
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
                Datos actualizados del usuario:
                $userFinancialData
                
                Consulta del usuario: "$userMessage"
                
                Como asesor financiero, proporciona:
                1. Análisis específico basado en estos datos
                2. 2-3 recomendaciones concretas
                3. Sugerencias para mejorar sus finanzas
                4. Máximo 250 palabras
                5. Enfócate en sus patrones de gastos/ingresos
            """.trimIndent()

            val response = chat.sendMessage(prompt)
            response.text ?: "No pude generar recomendaciones. Por favor, intenta con otra consulta."
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