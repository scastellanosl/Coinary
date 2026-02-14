package com.example.coinary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Debt
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.model.SavingsGoal
import com.example.coinary.utils.NotificationScheduler
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class DebtGoalUiState(
    val debts: List<Debt> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentBalance: Double = 0.0
)

class DebtGoalViewModel(application: Application) : AndroidViewModel(application) {

    private val firestoreManager = FirestoreManager()

    private val _uiState = MutableStateFlow(DebtGoalUiState())
    val uiState: StateFlow<DebtGoalUiState> = _uiState.asStateFlow()

    private var totalIncome = 0.0
    private var totalExpense = 0.0

    init {
        loadDebts()
        loadGoals()
        monitorBalance()
    }

    // --- MONITOREO DE SALDO ---
    private fun monitorBalance() {
        firestoreManager.getTotalIncomesRealtime(
            onTotalIncomeLoaded = { income ->
                totalIncome = income
                updateLocalBalance()
            },
            onFailure = { /* Manejo silencioso */ }
        )

        firestoreManager.getTotalExpensesRealtime(
            onTotalExpenseLoaded = { expense ->
                totalExpense = expense
                updateLocalBalance()
            },
            onFailure = { /* Manejo silencioso */ }
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

    fun addDebt(amount: Double, description: String, creditor: String, dueDate: Date) {
        if (amount <= 0 || description.isBlank() || creditor.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Completa todos los campos correctamente")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val debt = Debt(
                id = "",
                amount = amount,
                description = description,
                creditor = creditor,
                dueDate = Timestamp(dueDate),
                amountPaid = 0.0,
                isPaid = false
            )

            firestoreManager.addDebt(
                debt = debt,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Deuda creada exitosamente"
                    )

                    // PROGRAMAR NOTIFICACIONES AUTOMÁTICAS
                    NotificationScheduler.scheduleDebtReminders(
                        context = getApplication<Application>().applicationContext,
                        debtId = debt.id,
                        debtDescription = debt.description,
                        dueDate = dueDate
                    )

                    loadDebts()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al crear deuda: ${exception.message}"
                    )
                }
            )
        }
    }

    fun makePayment(debtId: String, paymentAmount: Double, isNewIncome: Boolean) {
        // Obtener la deuda actual
        val debt = _uiState.value.debts.find { it.id == debtId }

        if (debt == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Deuda no encontrada")
            return
        }

        //  VALIDACIÓN: No permitir pagos si ya está completada
        if (debt.isPaid) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Esta deuda ya está pagada completamente."
            )
            return
        }

        //  VALIDACIÓN: No permitir pagar más de lo que falta
        val remainingAmount = debt.amount - debt.amountPaid
        if (paymentAmount > remainingAmount) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Solo debes pagar ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(remainingAmount).replace("COP", "").trim()} para liquidar esta deuda."
            )
            return
        }

        // VALIDACIÓN DE SALDO
        if (!isNewIncome && paymentAmount > _uiState.value.currentBalance) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Saldo insuficiente. Tienes: $${_uiState.value.currentBalance.toInt()}"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        val debtDesc = debt.description

        if (isNewIncome) {
            val newIncome = Income(
                id = "",
                amount = paymentAmount,
                description = "Ingreso para pago: $debtDesc",
                category = "Otros",
                date = Timestamp.now()
            )
            firestoreManager.addIncome(newIncome, {}, {})
        }

        val newExpense = Expense(
            id = "",
            amount = paymentAmount,
            description = "Pago deuda $debtDesc",
            category = "Pago Deuda",
            date = Timestamp.now()
        )
        firestoreManager.addExpense(newExpense, {}, {})

        firestoreManager.makeDebtPayment(
            debtId = debtId,
            paymentAmount = paymentAmount,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Pago registrado exitosamente",
                    isLoading = false
                )

                //  Si se pagó completamente, cancelar notificaciones pendientes
                val newAmountPaid = debt.amountPaid + paymentAmount
                if (newAmountPaid >= debt.amount) {
                    NotificationScheduler.cancelDebtReminders(
                        context = getApplication<Application>().applicationContext,
                        debtId = debtId
                    )
                }
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
        firestoreManager.updateDebt(
            debt.copy(dueDate = Timestamp(newDate)),
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Fecha actualizada") },
            onFailure = { _uiState.value = _uiState.value.copy(errorMessage = "Error al actualizar") }
        )
    }

    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            firestoreManager.deleteDebt(
                debtId = debtId,
                onSuccess = {
                    //  CANCELAR NOTIFICACIONES PROGRAMADAS
                    NotificationScheduler.cancelDebtReminders(
                        context = getApplication<Application>().applicationContext,
                        debtId = debtId
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Deuda eliminada"
                    )
                    loadDebts()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al eliminar deuda: ${exception.message}"
                    )
                }
            )
        }
    }

    // ========== METAS ==========

    fun loadGoals() {
        firestoreManager.getSavingsGoalsRealtime(
            onGoalsLoaded = { goals ->
                _uiState.value = _uiState.value.copy(goals = goals, isLoading = false)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(errorMessage = "Error: ${e.message}")
            }
        )
    }

    fun addGoal(name: String, targetAmount: Double, deadline: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val newGoal = SavingsGoal(
            id = "",
            name = name,
            targetAmount = targetAmount,
            currentAmount = 0.0,
            deadline = Timestamp(deadline),
            isCompleted = false
        )

        firestoreManager.addSavingsGoal(
            newGoal,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Meta creada",
                    isLoading = false
                )
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message,
                    isLoading = false
                )
            }
        )
    }

    fun contributeToGoal(goalId: String, contributionAmount: Double, isNewIncome: Boolean) {
        if (contributionAmount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Monto inválido")
            return
        }

        // Obtener la meta actual ANTES de validar
        val currentGoal = _uiState.value.goals.find { it.id == goalId }

        if (currentGoal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Meta no encontrada")
            return
        }

        //  VALIDACIÓN: No permitir aportes si ya está completada
        if (currentGoal.isCompleted) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Esta meta ya está completada. No puedes aportar más."
            )
            return
        }

        //  VALIDACIÓN: No permitir aportar más de lo necesario
        val remainingAmount = currentGoal.targetAmount - currentGoal.currentAmount
        if (contributionAmount > remainingAmount) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Solo necesitas aportar ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).format(remainingAmount).replace("COP", "").trim()} para completar esta meta."
            )
            return
        }

        // VALIDACIÓN DE SALDO
        if (!isNewIncome && contributionAmount > _uiState.value.currentBalance) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Saldo insuficiente. Tienes: $${_uiState.value.currentBalance.toInt()}"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Registrar el gasto
            val newExpense = Expense(
                id = "",
                amount = contributionAmount,
                description = "Aporte a meta: ${currentGoal.name}",
                category = "Ahorro",
                date = Timestamp.now()
            )
            firestoreManager.addExpense(newExpense, {}, {})

            if (isNewIncome) {
                val newIncome = Income(
                    id = "",
                    amount = contributionAmount,
                    description = "Ingreso para aporte: ${currentGoal.name}",
                    category = "Otros",
                    date = Timestamp.now()
                )
                firestoreManager.addIncome(newIncome, {}, {})
            }

            firestoreManager.addToSavingsGoal(
                goalId = goalId,
                amountToAdd = contributionAmount,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Aporte realizado exitosamente"
                    )

                    //  VERIFICAR SI SE COMPLETÓ LA META (solo si llega EXACTAMENTE al objetivo)
                    val newCurrentAmount = currentGoal.currentAmount + contributionAmount
                    if (newCurrentAmount >= currentGoal.targetAmount && !currentGoal.isCompleted) {
                        //  META COMPLETADA
                        val context = getApplication<Application>().applicationContext

                        // Mostrar notificación inmediata
                        NotificationScheduler.showGoalCompletedNotification(
                            context = context,
                            goalId = currentGoal.id,
                            goalName = currentGoal.name,
                            targetAmount = currentGoal.targetAmount
                        )

                        //  GUARDAR EN EL HISTORIAL DE NOTIFICACIONES
                        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                        val currentDateTime = dateFormat.format(java.util.Date())
                        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("es", "CO")).apply {
                            maximumFractionDigits = 0
                        }
                        val formattedAmount = currencyFormat.format(currentGoal.targetAmount).replace("COP", "").trim()

                        val reminder = com.example.coinary.data.ReminderItem(
                            id = System.currentTimeMillis(),
                            title = "Meta completada",
                            message = "Felicitaciones Has completado tu meta de ahorro \"${currentGoal.name}\" alcanzando $formattedAmount. ¡Sigue así! ",
                            dateTime = currentDateTime
                        )
                        com.example.coinary.utils.ReminderStorage.addReminder(context, reminder)
                    }

                    loadGoals()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al aportar: ${exception.message}"
                    )
                }
            )
        }
    }

    fun updateGoalDate(goalId: String, newDate: Date) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        firestoreManager.updateSavingsGoal(
            goal.copy(deadline = Timestamp(newDate)),
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Fecha actualizada") },
            onFailure = { _uiState.value = _uiState.value.copy(errorMessage = "Error al actualizar") }
        )
    }

    fun resetMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
