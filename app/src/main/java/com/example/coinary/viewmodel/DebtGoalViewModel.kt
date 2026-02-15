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
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/**
 * UI State holding the list of debts, goals, and current financial status.
 */
data class DebtGoalUiState(
    val debts: List<Debt> = emptyList(),
    val goals: List<SavingsGoal> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val currentBalance: Double = 0.0
)

/**
 * DebtGoalViewModel: Manages the business logic for Debts and Savings Goals.
 * Handles Firestore interactions, balance calculation, and notification scheduling.
 * Includes logic to delete records and update due dates.
 *
 * @param application The application context, used for accessing system services.
 */
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

    // --- BALANCE MONITORING ---

    /**
     * Subscribes to real-time updates for Income and Expenses to calculate the net balance.
     */
    private fun monitorBalance() {
        firestoreManager.getTotalIncomesRealtime(
            onTotalIncomeLoaded = { income ->
                totalIncome = income
                updateLocalBalance()
            },
            onFailure = { /* Silent failure */ }
        )

        firestoreManager.getTotalExpensesRealtime(
            onTotalExpenseLoaded = { expense ->
                totalExpense = expense
                updateLocalBalance()
            },
            onFailure = { /* Silent failure */ }
        )
    }

    private fun updateLocalBalance() {
        val balance = totalIncome - totalExpense
        _uiState.value = _uiState.value.copy(currentBalance = balance)
    }

    // ============================================================================================
    // REGION: DEBTS
    // ============================================================================================

    fun loadDebts() {
        firestoreManager.getDebtsRealtime(
            onDebtsLoaded = { debtsList ->
                _uiState.value = _uiState.value.copy(debts = debtsList, isLoading = false)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(errorMessage = "Error loading debts: ${e.message}", isLoading = false)
            }
        )
    }

    fun addDebt(amount: Double, description: String, creditor: String, dueDate: Date) {
        if (amount <= 0 || description.isBlank() || creditor.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please complete all fields correctly.")
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
                        successMessage = "Debt created successfully"
                    )

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
                        errorMessage = "Error creating debt: ${exception.message}"
                    )
                }
            )
        }
    }

    fun makePayment(debtId: String, paymentAmount: Double, isNewIncome: Boolean) {
        val debt = _uiState.value.debts.find { it.id == debtId }

        if (debt == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Debt not found")
            return
        }

        if (debt.isPaid) {
            _uiState.value = _uiState.value.copy(errorMessage = "This debt is already fully paid.")
            return
        }

        val remainingAmount = debt.amount - debt.amountPaid
        if (paymentAmount > remainingAmount) {
            val formattedRemaining = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(remainingAmount)
            _uiState.value = _uiState.value.copy(
                errorMessage = "You only need to pay $formattedRemaining to settle this debt."
            )
            return
        }

        if (!isNewIncome && paymentAmount > _uiState.value.currentBalance) {
            val formattedBalance = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(_uiState.value.currentBalance)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Insufficient balance. You have: $formattedBalance"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)
        val debtDesc = debt.description

        if (isNewIncome) {
            val newIncome = Income(
                id = "",
                amount = paymentAmount,
                description = "Income for debt: $debtDesc",
                category = "Other",
                date = Timestamp.now()
            )
            firestoreManager.addIncome(newIncome, {}, {})
        }

        val newExpense = Expense(
            id = "",
            amount = paymentAmount,
            description = "Payment for debt: $debtDesc",
            category = "Debt Payment",
            date = Timestamp.now()
        )
        firestoreManager.addExpense(newExpense, {}, {})

        firestoreManager.makeDebtPayment(
            debtId = debtId,
            paymentAmount = paymentAmount,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    successMessage = "Payment registered successfully",
                    isLoading = false
                )

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

    /**
     * Updates the due date of a debt and reschedules reminders accordingly.
     */
    fun updateDebtDate(debtId: String, newDate: Date) {
        val debt = _uiState.value.debts.find { it.id == debtId } ?: return
        firestoreManager.updateDebt(
            debt.copy(dueDate = Timestamp(newDate)),
            onSuccess = {
                val context = getApplication<Application>().applicationContext
                // Cancel old reminders and set new ones for the updated date
                NotificationScheduler.cancelDebtReminders(context, debtId)
                NotificationScheduler.scheduleDebtReminders(context, debtId, debt.description, newDate)

                _uiState.value = _uiState.value.copy(successMessage = "Date updated successfully")
            },
            onFailure = { _uiState.value = _uiState.value.copy(errorMessage = "Error updating date") }
        )
    }

    /**
     * Deletes a debt record and cancels all associated notifications.
     */
    fun deleteDebt(debtId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            firestoreManager.deleteDebt(
                debtId = debtId,
                onSuccess = {
                    NotificationScheduler.cancelDebtReminders(
                        context = getApplication<Application>().applicationContext,
                        debtId = debtId
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Debt deleted successfully"
                    )
                    loadDebts()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error deleting debt: ${exception.message}"
                    )
                }
            )
        }
    }

    // ============================================================================================
    // REGION: SAVINGS GOALS
    // ============================================================================================

    fun loadGoals() {
        firestoreManager.getSavingsGoalsRealtime(
            onGoalsLoaded = { goals ->
                _uiState.value = _uiState.value.copy(goals = goals, isLoading = false)
            },
            onFailure = { e ->
                _uiState.value = _uiState.value.copy(errorMessage = "Error loading goals: ${e.message}")
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
                    successMessage = "Goal created successfully",
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
            _uiState.value = _uiState.value.copy(errorMessage = "Invalid amount")
            return
        }

        val currentGoal = _uiState.value.goals.find { it.id == goalId }

        if (currentGoal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Goal not found")
            return
        }

        if (currentGoal.isCompleted) {
            _uiState.value = _uiState.value.copy(errorMessage = "This goal is already completed.")
            return
        }

        val remainingAmount = currentGoal.targetAmount - currentGoal.currentAmount
        if (contributionAmount > remainingAmount) {
            val formattedRemaining = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(remainingAmount)
            _uiState.value = _uiState.value.copy(
                errorMessage = "You only need to contribute $formattedRemaining to complete this goal."
            )
            return
        }

        if (!isNewIncome && contributionAmount > _uiState.value.currentBalance) {
            val formattedBalance = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(_uiState.value.currentBalance)
            _uiState.value = _uiState.value.copy(
                errorMessage = "Insufficient balance. You have: $formattedBalance"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val newExpense = Expense(
                id = "",
                amount = contributionAmount,
                description = "Contribution to goal: ${currentGoal.name}",
                category = "Savings",
                date = Timestamp.now()
            )
            firestoreManager.addExpense(newExpense, {}, {})

            if (isNewIncome) {
                val newIncome = Income(
                    id = "",
                    amount = contributionAmount,
                    description = "Income for goal: ${currentGoal.name}",
                    category = "Other",
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
                        successMessage = "Contribution successful"
                    )

                    val newCurrentAmount = currentGoal.currentAmount + contributionAmount
                    if (newCurrentAmount >= currentGoal.targetAmount && !currentGoal.isCompleted) {
                        NotificationScheduler.showGoalCompletedNotification(
                            context = getApplication<Application>().applicationContext,
                            goalId = currentGoal.id,
                            goalName = currentGoal.name,
                            targetAmount = currentGoal.targetAmount
                        )
                    }

                    loadGoals()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error contributing: ${exception.message}"
                    )
                }
            )
        }
    }

    /**
     * Updates the deadline for a specific savings goal.
     */
    fun updateGoalDate(goalId: String, newDate: Date) {
        val goal = _uiState.value.goals.find { it.id == goalId } ?: return
        firestoreManager.updateSavingsGoal(
            goal.copy(deadline = Timestamp(newDate)),
            onSuccess = { _uiState.value = _uiState.value.copy(successMessage = "Goal date updated successfully") },
            onFailure = { _uiState.value = _uiState.value.copy(errorMessage = "Error updating goal date") }
        )
    }

    /**
     * Deletes a savings goal record from the user's collection.
     */
    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            firestoreManager.deleteSavingsGoal(
                goalId = goalId,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Goal deleted successfully"
                    )
                    loadGoals()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error deleting goal: ${e.message}"
                    )
                }
            )
        }
    }

    /**
     * Resets the error and success messages to prevent them from showing repeatedly.
     */
    fun resetMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}