package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Debt
import com.example.coinary.model.Expense // Importante: Importar Expense
import com.example.coinary.model.Income
import com.example.coinary.model.SavingsGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

data class DebtGoalUiState(
    val debts: List<Debt> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentBalance: Double = 0.0 // Nuevo: Guardamos el saldo actual para validar
)

class DebtGoalViewModel : ViewModel() {

    private val firestoreManager = FirestoreManager()

    private val _uiState = MutableStateFlow(DebtGoalUiState())
    val uiState: StateFlow<DebtGoalUiState> = _uiState.asStateFlow()

    // Variables internas para calcular el balance
    private var totalIncome = 0.0
    private var totalExpense = 0.0

    init {
        loadDebts()
        loadGoals()
        monitorBalance() // Iniciamos el monitoreo del saldo
    }

    // --- MONITOREO DE SALDO (Para validación) ---
    private fun monitorBalance() {
        // Escuchamos ingresos totales
        firestoreManager.getTotalIncomesRealtime(
            onTotalIncomeLoaded = { income ->
                totalIncome = income
                updateLocalBalance()
            },
            onFailure = { /* Manejo silencioso o log */ }
        )

        // Escuchamos gastos totales
        firestoreManager.getTotalExpensesRealtime(
            onTotalExpenseLoaded = { expense ->
                totalExpense = expense
                updateLocalBalance()
            },
            onFailure = { /* Manejo silencioso o log */ }
        )
    }

    private fun updateLocalBalance() {
        val balance = totalIncome - totalExpense
        _uiState.value = _uiState.value.copy(currentBalance = balance)
    }

    // ========== DEUDAS ==========

    fun loadDebts() {
        firestoreManager.getDebtsRealtime(
            onDebtsLoaded = { debtsList ->
                _uiState.value = _uiState.value.copy(debts = debtsList, isLoading = false)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(errorMessage = "Error: ${e.message}", isLoading = false)
            }
        )
    }

    fun addDebt(amount: Double, description: String, creditor: String, date: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // USO DE ARGUMENTOS NOMBRADOS PARA EVITAR EL ERROR
        val newDebt = Debt(
            id = "",
            amount = amount,
            amountPaid = 0.0,
            description = description,
            creditor = creditor,
            dueDate = com.google.firebase.Timestamp(date),
            isPaid = false
        )

        firestoreManager.addDebt(newDebt,
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Deuda creada", isLoading = false) },
            onFailure = { e -> _uiState.value = _uiState.value.copy(errorMessage = e.message, isLoading = false) }
        )
    }

    fun makePayment(debtId: String, paymentAmount: Double, isNewIncome: Boolean) {
        // 1. VALIDACIÓN DE SALDO
        if (!isNewIncome && paymentAmount > _uiState.value.currentBalance) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Saldo insuficiente. Tienes: $${_uiState.value.currentBalance.toInt()}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        val debtDesc = _uiState.value.debts.find { it.id == debtId }?.description ?: "Deuda"

        // 2. LÓGICA DE MOVIMIENTOS

        if (isNewIncome) {
            val newIncome = Income(
                id = "",
                amount = paymentAmount,
                description = "Ingreso para pago: $debtDesc",
                category = "Otros",
                date = com.google.firebase.Timestamp.now()
            )
            firestoreManager.addIncome(newIncome, {}, {})
        }

        // CORRECCIÓN AQUÍ TAMBIÉN: Argumentos nombrados para Expense
        val newExpense = Expense(
            id = "",
            amount = paymentAmount,
            description = "Pago de deuda: $debtDesc",
            category = "Otros",
            date = com.google.firebase.Timestamp.now()
        )
        firestoreManager.addExpense(newExpense, {}, {})

        // 3. ACTUALIZAR LA DEUDA
        firestoreManager.makeDebtPayment(
            debtId = debtId,
            paymentAmount = paymentAmount,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Pago registrado exitosamente",
                    isLoading = false
                )
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}",
                    isLoading = false
                )
            }
        )
    }

    fun updateDebtDate(debtId: String, newDate: Date) {
        val debt = _uiState.value.debts.find { it.id == debtId } ?: return
        firestoreManager.updateDebt(debt.copy(dueDate = com.google.firebase.Timestamp(newDate)),
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Fecha actualizada") },
            onFailure = { _uiState.value = _uiState.value.copy(errorMessage = "Error al actualizar") }
        )
    }

    // ========== METAS ==========

    fun loadGoals() {
        firestoreManager.getSavingsGoalsRealtime(
            onGoalsLoaded = { goals -> _uiState.value = _uiState.value.copy(goals = goals, isLoading = false) },
            onFailure = { e -> _uiState.value = _uiState.value.copy(errorMessage = "Error: ${e.message}") }
        )
    }

    fun addGoal(name: String, targetAmount: Double, deadline: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        // USO DE ARGUMENTOS NOMBRADOS
        val newGoal = SavingsGoal(
            id = "",
            name = name,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            deadline = com.google.firebase.Timestamp(deadline),
            isCompleted = false
        )

        firestoreManager.addSavingsGoal(newGoal,
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Meta creada", isLoading = false) },
            onFailure = { e -> _uiState.value = _uiState.value.copy(errorMessage = e.message, isLoading = false) }
        )
    }

    fun contributeToGoal(goalId: String, contributionAmount: Double, isNewIncome: Boolean) {
        // 1. VALIDACIÓN DE SALDO
        if (!isNewIncome && contributionAmount > _uiState.value.currentBalance) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Saldo insuficiente. Tienes: $${_uiState.value.currentBalance.toInt()}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        val goalName = _uiState.value.goals.find { it.id == goalId }?.name ?: "Meta"

        // 2. LÓGICA DE MOVIMIENTOS

        if (isNewIncome) {
            // Argumentos nombrados para Income
            val newIncome = Income(
                id = "",
                amount = contributionAmount,
                description = "Ingreso para meta: $goalName",
                category = "Otros",
                date = com.google.firebase.Timestamp.now()
            )
            firestoreManager.addIncome(newIncome, {}, {})
        }

        // Argumentos nombrados para Expense
        val newExpense = Expense(
            id = "",
            amount = contributionAmount,
            description = "Aporte a meta: $goalName",
            category = "Otros",
            date = com.google.firebase.Timestamp.now()
        )
        firestoreManager.addExpense(newExpense, {}, {})

        // 3. ACTUALIZAR LA META
        firestoreManager.addToSavingsGoal(
            goalId = goalId,
            amountToAdd = contributionAmount,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Aporte registrado exitosamente",
                    isLoading = false
                )
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}",
                    isLoading = false
                )
            }
        )
    }

    fun updateGoalDate(goalId: String, newDate: Date) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        firestoreManager.updateSavingsGoal(goal.copy(deadline = com.google.firebase.Timestamp(newDate)),
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Fecha actualizada") },
            onFailure = { _uiState.value = _uiState.value.copy(errorMessage = "Error al actualizar") }
        )
    }

    fun resetMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}