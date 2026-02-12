package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Debt
import com.example.coinary.model.SavingsGoal
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
    val successMessage: String? = null
)

class DebtGoalViewModel : ViewModel() {

    private val firestoreManager = FirestoreManager()

    private val _uiState = MutableStateFlow(DebtGoalUiState())
    val uiState: StateFlow<DebtGoalUiState> = _uiState.asStateFlow()

    init {
        loadDebts()
        loadGoals()
    }

    // ========== DEUDAS ==========

    fun loadDebts() {
        firestoreManager.getDebtsRealtime(
            onDebtsLoaded = { debtsList ->
                _uiState.value = _uiState.value.copy(
                    debts = debtsList,
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al cargar deudas: ${exception.message}",
                    isLoading = false
                )
            }
        )
    }

    fun makePayment(debtId: String, paymentAmount: Double) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        firestoreManager.makeDebtPayment(
            debtId = debtId,
            paymentAmount = paymentAmount,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Pago registrado exitosamente",
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al registrar pago: ${exception.message}",
                    isLoading = false
                )
            }
        )
    }

    fun updateDebtDate(debtId: String, newDate: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val debt = _uiState.value.debts.find { it.id == debtId } ?: return
        val updatedDebt = debt.copy(dueDate = com.google.firebase.Timestamp(newDate))

        firestoreManager.updateDebt(
            debt = updatedDebt,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Fecha actualizada exitosamente",
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al actualizar fecha: ${exception.message}",
                    isLoading = false
                )
            }
        )
    }

    // ========== METAS ==========

    fun loadGoals() {
        firestoreManager.getSavingsGoalsRealtime(
            onGoalsLoaded = { goalsList ->
                _uiState.value = _uiState.value.copy(
                    goals = goalsList,
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al cargar metas: ${exception.message}",
                    isLoading = false
                )
            }
        )
    }

    fun contributeToGoal(goalId: String, contributionAmount: Double) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        firestoreManager.addToSavingsGoal(
            goalId = goalId,
            amountToAdd = contributionAmount,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Aporte registrado exitosamente",
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al registrar aporte: ${exception.message}",
                    isLoading = false
                )
            }
        )
    }

    fun updateGoalDate(goalId: String, newDate: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        val updatedGoal = goal.copy(deadline = com.google.firebase.Timestamp(newDate))

        firestoreManager.updateSavingsGoal(
            goal = updatedGoal,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Fecha actualizada exitosamente",
                    isLoading = false
                )
            },
            onFailure = { exception ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al actualizar fecha: ${exception.message}",
                    isLoading = false
                )
            }
        )
    }

    fun resetMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
