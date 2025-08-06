package com.example.coinary.viewmodel

import android.R.attr.category
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
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt

data class RecommendationUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hola, soy tu asesor financiero personal. Estoy analizando tus movimientos para darte recomendaciones precisas y personalizadas. Dame un momento mientras cargo tus datos...", false)
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
    private var userFinancialData: String = ""
    private val scope = MainScope()

    init {
        initializeGemini()
        loadUserFinancialData()
    }

    private fun initializeGemini() {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY
        )

        val initialPrompt = """
            Eres un asesor financiero especializado en finanzas personales con estas reglas estrictas:
            
            1. dale a conocer al usuario los datos que ha gastado como un balance general
            1. **Enfoque**: Solo responder sobre gestión financiera personal
            2. **Base de datos**: Usar exclusivamente los datos proporcionados
            3. **Formato**:
               - Sin negritas, asteriscos o markdown
               - Estructura clara con viñetas
            4. **Recomendaciones**:
               - Siempre proporcionar 3-5 recomendaciones accionables
               - Priorizar por impacto potencial
               - Incluir pasos concretos
               - Basar cada sugerencia en datos específicos
            5. **Patrones peligrosos**: 
               - Señalar claramente cualquier patrón riesgoso
               - Explicar consecuencias potenciales
            6. **Lenguaje**: 
               - Claro y directo
               - Evitar jerga financiera compleja
               - razona para entender la jerga o habla de las personas
               - Tono empático pero profesional
            
            Los datos históricos del usuario serán proporcionados a continuación.
        """.trimIndent()

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text(initialPrompt) },
                content(role = "model") {
                    text("Entendido. Operaré como asesor financiero personal con respuestas claras, prácticas y basadas en datos. Por favor, proporcióname tu consulta una vez carguemos tus datos históricos.")
                }
            )
        )
    }

    private fun loadUserFinancialData() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        scope.launch {
            try {
                // Obtener datos de los últimos 6 meses para análisis completo
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
                        ChatMessage("He analizado tus últimos meses. ¿En qué área necesitas recomendaciones? Por ejemplo: 'Quiero optimizar mis ahorros'", false)
                    ),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error cargando datos. Por favor, verifica tu conexión.",
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
        sb.appendln("Análisis Financiero Detallado:")

        // 1. Resumen general
        val allIncomes = historicalData.flatMap { it.first }
        val allExpenses = historicalData.flatMap { it.second }
        val totalIncome = allIncomes.sumOf { it.amount }
        val totalExpense = allExpenses.sumOf { it.amount }

        sb.appendln("\n● Resumen de los últimos ${historicalData.size} meses:")
        sb.appendln("- Ingresos totales: ${"%.2f".format(totalIncome)}")
        sb.appendln("- Gastos totales: ${"%.2f".format(totalExpense)}")

        // 2. Análisis de gastos por categoría
        if (allExpenses.isNotEmpty()) {
            val expenseAnalysis = allExpenses.groupBy { it.category }
                .mapValues { (_, expenses) ->
                    val total = expenses.sumOf { it.amount }
                    val monthlyAvg = total / historicalData.size
                    Pair(total, monthlyAvg)
                }
                .entries.sortedByDescending { it.value.first }

            sb.appendln("\n● Distribución de gastos:")
            expenseAnalysis.forEach { entry ->
                val category = entry.key
                val total = entry.value.first
                val monthlyAvg = entry.value.second
                sb.appendln("- $category: ${"%.2f".format(total)} total (${"%.2f".format(monthlyAvg)}/mes)")
            }

            // 3. Detección de patrones
            sb.appendln("\n● Patrones detectados:")
            val variableExpenses = expenseAnalysis.filter { it.key !in listOf("Vivienda", "Transporte") }
            val highestVariable = variableExpenses.firstOrNull()

            highestVariable?.let {
                sb.appendln("- Mayor gasto variable: ${it.key} (${"%.2f".format(it.value.second)}/mes)")
            }

            // Gastos hormiga
            val smallFrequentExpenses = allExpenses
                .groupBy { it.description }
                .filter { it.value.size > 2 && it.value.sumOf { e -> e.amount } < totalExpense * 0.05 }

            if (smallFrequentExpenses.isNotEmpty()) {
                sb.appendln("- Posibles gastos hormiga: ${smallFrequentExpenses.size} distintos identificados")
            }
        }

        // 4. Tendencias temporales
        sb.appendln("\n● Evolución mensual:")
        historicalData.forEachIndexed { i, (incomes, expenses) ->
            val monthName = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
                .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            val savings = incomes.sumOf { it.amount } - expenses.sumOf { it.amount }
            sb.appendln("- $monthName: ${"%.2f".format(savings)} (${"%.0f".format(savings*100/incomes.sumOf { it.amount })}% de ahorro)")
        }

        return sb.toString()
    }

    fun sendMessage(message: String) {
        if (message.isBlank() || _uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(message, true),
            isLoading = true,
            errorMessage = null
        )

        scope.launch {
            try {
                val prompt = """
                    Datos completos del usuario:
                    $userFinancialData
                    
                    Consulta específica: "$message"
                    
                    Por favor:
                    1. Analiza los patrones financieros
                    2. Proporciona 3-5 recomendaciones accionables
                    3. Señala cualquier patrón riesgoso
                    4. Mantén un lenguaje claro sin formatos especiales
                """.trimIndent()

                val response = chat.sendMessage(prompt)
                val cleanResponse = response.text
                    ?.replace("**", "") // Eliminar negritas
                    ?.replace("*", "•") // Convertir asteriscos a viñetas
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