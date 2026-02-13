package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Debt
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

// --- ESTADO DE LA UI ---
data class MovementUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val antExpenses: List<Expense> = emptyList(), // Lista real para la pantalla de Hormiga
    val showDebtConfirmation: Boolean = false,    // Controla la alerta de saldo insuficiente
    val tempExpenseData: Expense? = null          // Guarda el gasto temporalmente mientras decides
)

class MovementViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovementUiState())
    val uiState: StateFlow<MovementUiState> = _uiState.asStateFlow()

    init {
        // Cargar gastos hormiga automáticamente al iniciar
        loadAntExpenses()
    }

    private fun loadAntExpenses() {
        firestoreManager.getAntExpensesRealtime(
            onExpensesLoaded = { list ->
                _uiState.update { it.copy(antExpenses = list) }
            },
            onFailure = { /* Error silencioso o log */ }
        )
    }

    fun saveIncome(amount: Double, description: String, category: String, date: Date) {
        _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }
        viewModelScope.launch {
            val income = Income(amount = amount, description = description, category = category, date = Timestamp(date))
            firestoreManager.addIncome(income,
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, successMessage = "Ingreso guardado exitosamente.") }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
            )
        }
    }

    // --- LÓGICA DE VERIFICACIÓN DE SALDO ---
    // Esta es la función que llama tu botón "Guardar" en AddMovementScreen
    fun checkAndSaveExpense(amount: Double, description: String, category: String, date: Date) {
        _uiState.update { it.copy(isLoading = true, successMessage = null, errorMessage = null) }

        viewModelScope.launch {
            // 1. Obtenemos Ingresos Totales
            firestoreManager.getTotalIncomesRealtime(
                onTotalIncomeLoaded = { totalIncome ->
                    // 2. Obtenemos Gastos Totales
                    firestoreManager.getTotalExpensesRealtime(
                        onTotalExpenseLoaded = { totalExpense ->
                            val currentBalance = totalIncome - totalExpense

                            // 3. Verificamos si hay fondos
                            if (amount > currentBalance) {
                                // NO HAY FONDOS: Preguntar si crear deuda
                                val tempExpense = Expense(
                                    amount = amount,
                                    description = description,
                                    category = category,
                                    date = Timestamp(date)
                                )
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        showDebtConfirmation = true,
                                        tempExpenseData = tempExpense
                                    )
                                }
                            } else {
                                // HAY FONDOS: Proceder con el guardado inteligente
                                saveExpensePro(amount, description, category, date)
                            }
                        },
                        onFailure = {
                            // Si falla calcular gastos, intentamos guardar igual
                            saveExpensePro(amount, description, category, date)
                        }
                    )
                },
                onFailure = {
                    // Si falla calcular ingresos, intentamos guardar igual
                    saveExpensePro(amount, description, category, date)
                }
            )
        }
    }

    // --- ACCIONES DE LA ALERTA DE DEUDA ---
    fun confirmDebtCreation() {
        val tempExpense = _uiState.value.tempExpenseData ?: return

        val newDebt = Debt(
            amount = tempExpense.amount,
            description = "Gasto sin fondos: ${tempExpense.description}",
            creditor = "Auto-generado (Coinary)",
            dueDate = Timestamp.now(),
            isPaid = false
        )

        _uiState.update { it.copy(isLoading = true, showDebtConfirmation = false) }

        firestoreManager.addDebt(newDebt,
            onSuccess = {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Se ha creado una deuda automáticamente.",
                        tempExpenseData = null
                    )
                }
            },
            onFailure = { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        )
    }

    fun cancelDebtCreation() {
        _uiState.update { it.copy(showDebtConfirmation = false, tempExpenseData = null) }
    }

    // --- LÓGICA "PRO" DE GASTO HORMIGA (FRECUENCIA + MONTO) ---
    // Esta función se llama internamente si hay saldo suficiente
    // En MovementViewModel.kt

    fun saveExpensePro(amount: Double, description: String, category: String, date: Date) {
        if (!_uiState.value.isLoading) _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            // 1. Consultamos el historial de los últimos 7 días
            firestoreManager.getRecentExpensesCount(
                category = category,
                onResult = { pastFrequency ->

                    // 2. NUEVA REGLA: LÍMITE DE $9.000 + RECURRENCIA
                    // pastFrequency = Veces que compraste antes.
                    // Si ya compraste 2 veces antes (pastFrequency >= 2), esta es la 3ra.

                    val maxAmountForAnt = 9000.0 // <--- NUEVO LÍMITE SOLICITADO

                    val isAnt = if (amount <= maxAmountForAnt && pastFrequency >= 2) {
                        true // Es barato (<= 9.000) Y repetitivo (3ra vez o más) -> ¡HORMIGA!
                    } else {
                        false // Es muy caro o es la primera vez -> NORMAL
                    }

                    // 3. Crear objeto
                    val expense = Expense(
                        amount = amount,
                        description = description,
                        category = category,
                        date = Timestamp(date),
                        isAntExpense = isAnt
                    )

                    // 4. Guardar
                    firestoreManager.addExpense(expense,
                        onSuccess = {
                            val msg = if (isAnt) "⚠️ Hábito hormiga detectado ($category)" else "Gasto guardado."
                            _uiState.update { it.copy(isLoading = false, successMessage = msg) }
                        },
                        onFailure = { e ->
                            _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                        }
                    )
                },
                onFailure = {
                    saveExpenseFallback(amount, description, category, date)
                }
            )
        }
    }

    private fun saveExpenseFallback(amount: Double, description: String, category: String, date: Date) {
        val expense = Expense(
            amount = amount,
            description = description,
            category = category,
            date = Timestamp(date),
            isAntExpense = false // Siempre false en modo offline
        )

        firestoreManager.addExpense(expense,
            onSuccess = {
                _uiState.update { it.copy(isLoading = false, successMessage = "Gasto guardado (sin análisis).") }
            },
            onFailure = { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        )
    }

    fun saveExpense(amount: Double, description: String, category: String, date: Date) {
        saveExpensePro(amount, description, category, date)
    }

    fun resetMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}