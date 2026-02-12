package com.example.coinary.data

import com.example.coinary.model.Debt
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.example.coinary.model.SavingsGoal
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Locale

/**
 * Manages all data persistence interactions with Firebase Firestore for the Coinary application.
 * Handles CRUD operations and real-time analytics aggregations.
 */
class FirestoreManager {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Helper method to retrieve the currently authenticated user's unique ID (UID).
     * @return The UID string, or null if no user is logged in.
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // ============================================================================================
    // REGION: INCOME OPERATIONS
    // ============================================================================================

    /**
     * Persists a new [Income] record to the specific user's Firestore sub-collection.
     */
    fun addIncome(income: Income, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        val incomeData = hashMapOf(
            "amount" to income.amount,
            "description" to income.description,
            "category" to income.category,
            "date" to income.date,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("incomes")
            .add(incomeData)
            .addOnSuccessListener { documentReference ->
                income.id = documentReference.id
                println("Income added successfully with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error adding income: $e")
                onFailure(e)
            }
    }

    /**
     * Real-time listener for the user's income records.
     */
    fun getIncomesRealtime(
        onIncomesLoaded: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("incomes")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for incomes: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val incomesList = snapshots?.map { doc ->
                    doc.toObject(Income::class.java).apply { id = doc.id }
                } ?: emptyList()
                onIncomesLoaded(incomesList)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Calculates the sum of all income records (All-time) in real-time.
     */
    fun getTotalIncomesRealtime(
        onTotalIncomeLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("incomes")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for total incomes: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onTotalIncomeLoaded(total)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Calculates the sum of incomes within a specific date range in real-time.
     * Used for filtering totals by Day, Week, Month, etc.
     */
    fun getIncomeSumByDateRangeRealtime(
        startDate: Timestamp,
        endDate: Timestamp,
        onResult: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("incomes")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for income sum by date range: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onResult(total)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Updates an existing income document.
     */
    fun updateIncome(income: Income, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        if (income.id.isEmpty()) {
            onFailure(Exception("Income ID not provided for update."))
            return
        }

        val updates = hashMapOf<String, Any>(
            "amount" to income.amount,
            "description" to income.description,
            "category" to income.category,
            "date" to income.date
        )

        db.collection("users").document(userId)
            .collection("incomes").document(income.id)
            .update(updates)
            .addOnSuccessListener {
                println("Income updated successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error updating income: $e")
                onFailure(e)
            }
    }

    /**
     * Deletes a specific income document.
     */
    fun deleteIncome(incomeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        db.collection("users").document(userId)
            .collection("incomes").document(incomeId)
            .delete()
            .addOnSuccessListener {
                println("Income deleted successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error deleting income: $e")
                onFailure(e)
            }
    }

    // ============================================================================================
    // REGION: EXPENSE OPERATIONS
    // ============================================================================================

    /**
     * Persists a new [Expense] record to the specific user's Firestore sub-collection.
     */
    fun addExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        val expenseData = hashMapOf(
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "date" to expense.date,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("expenses")
            .add(expenseData)
            .addOnSuccessListener { documentReference ->
                expense.id = documentReference.id
                println("Expense added successfully with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error adding expense: $e")
                onFailure(e)
            }
    }

    /**
     * Real-time listener for the user's expense records.
     */
    fun getExpensesRealtime(
        onExpensesLoaded: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for expenses: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val expensesList = snapshots?.map { doc ->
                    doc.toObject(Expense::class.java).apply { id = doc.id }
                } ?: emptyList()
                onExpensesLoaded(expensesList)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Calculates the sum of all expense records (All-time) in real-time.
     */
    fun getTotalExpensesRealtime(
        onTotalExpenseLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("expenses")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for total expenses: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onTotalExpenseLoaded(total)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Calculates the sum of expenses within a specific date range in real-time.
     * Used for filtering totals by Day, Week, Month, etc.
     */
    fun getExpenseSumByDateRangeRealtime(
        startDate: Timestamp,
        endDate: Timestamp,
        onResult: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for expense sum by date range: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                // Sum all amounts in the filtered documents
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onResult(total)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Updates an existing expense document.
     */
    fun updateExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        if (expense.id.isEmpty()) {
            onFailure(Exception("Expense ID not provided for update."))
            return
        }

        val updates = hashMapOf<String, Any>(
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "date" to expense.date
        )

        db.collection("users").document(userId)
            .collection("expenses").document(expense.id)
            .update(updates)
            .addOnSuccessListener {
                println("Expense updated successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error updating expense: $e")
                onFailure(e)
            }
    }

    /**
     * Deletes a specific expense document.
     */
    fun deleteExpense(expenseId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        db.collection("users").document(userId)
            .collection("expenses").document(expenseId)
            .delete()
            .addOnSuccessListener {
                println("Expense deleted successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error deleting expense: $e")
                onFailure(e)
            }
    }

    // ============================================================================================
    // REGION: ANALYTICS & FILTERED QUERIES
    // ============================================================================================

    /**
     * Retrieves expenses aggregated by category for the current week (Monday to Sunday).
     */
    fun getWeeklyExpensesByCategoryRealtime(
        onCategorizedExpensesLoaded: (Map<String, Double>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.firstDayOfWeek = Calendar.MONDAY

        // Start of Week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = Timestamp(calendar.time)

        // End of Week
        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startOfWeek)
            .whereLessThanOrEqualTo("date", endOfWeek)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for weekly categorized expenses: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val categorizedExpenses = mutableMapOf<String, Double>()
                snapshots?.forEach { doc ->
                    val category = doc.getString("category") ?: "Uncategorized"
                    val amount = doc.getDouble("amount") ?: 0.0
                    categorizedExpenses[category] = (categorizedExpenses[category] ?: 0.0) + amount
                }
                onCategorizedExpensesLoaded(categorizedExpenses)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Retrieves expense amounts grouped by category within a specific date range.
     */
    fun getExpensesByCategoryByDateRangeRealtime(
        startDate: Timestamp,
        endDate: Timestamp,
        onCategorizedExpensesLoaded: (Map<String, Double>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for filtered expenses: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val categorizedExpenses = mutableMapOf<String, Double>()
                snapshots?.forEach { doc ->
                    val category = doc.getString("category") ?: "Uncategorized"
                    val amount = doc.getDouble("amount") ?: 0.0
                    categorizedExpenses[category] = (categorizedExpenses[category] ?: 0.0) + amount
                }
                onCategorizedExpensesLoaded(categorizedExpenses)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    // --- Legacy / One-Time Monthly Methods ---

    fun getMonthlyIncomesRealtime(
        month: Int,
        year: Int,
        onIncomesLoaded: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val (startOfMonth, endOfMonth) = calculateMonthRange(month, year)

        db.collection("users").document(userId)
            .collection("incomes")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { onFailure(e); return@addSnapshotListener }
                val incomesList = snapshots?.map { doc -> doc.toObject(Income::class.java).apply { id = doc.id } } ?: emptyList()
                onIncomesLoaded(incomesList)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    fun getMonthlyExpensesRealtime(
        month: Int,
        year: Int,
        onExpensesLoaded: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val (startOfMonth, endOfMonth) = calculateMonthRange(month, year)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { onFailure(e); return@addSnapshotListener }
                val expensesList = snapshots?.map { doc -> doc.toObject(Expense::class.java).apply { id = doc.id } } ?: emptyList()
                onExpensesLoaded(expensesList)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    fun getMonthlyIncomesOnce(
        month: Int,
        year: Int,
        onSuccess: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))
        val (startOfMonth, endOfMonth) = calculateMonthRange(month, year)

        db.collection("users").document(userId)
            .collection("incomes")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .get()
            .addOnSuccessListener { snapshots ->
                val incomesList = snapshots.map { doc -> doc.toObject(Income::class.java).apply { id = doc.id } }
                onSuccess(incomesList)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getMonthlyExpensesOnce(
        month: Int,
        year: Int,
        onSuccess: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))
        val (startOfMonth, endOfMonth) = calculateMonthRange(month, year)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .get()
            .addOnSuccessListener { snapshots ->
                val expensesList = snapshots.map { doc -> doc.toObject(Expense::class.java).apply { id = doc.id } }
                onSuccess(expensesList)
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Helper to calculate start/end Timestamps for a given month/year.
     */
    private fun calculateMonthRange(month: Int, year: Int): Pair<Timestamp, Timestamp> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val end = Timestamp(calendar.time)

        return Pair(start, end)
    }

    // ============================================================================================
// REGION: DEBT OPERATIONS
// ============================================================================================

    /**
     * Persists a new [Debt] record to the specific user's Firestore sub-collection.
     */
    fun addDebt(debt: Debt, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        val debtData = hashMapOf(
            "amount" to debt.amount,
            "description" to debt.description,
            "creditor" to debt.creditor,
            "dueDate" to debt.dueDate,
            "isPaid" to debt.isPaid,
            "amountPaid" to debt.amountPaid,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("debts")
            .add(debtData)
            .addOnSuccessListener { documentReference ->
                debt.id = documentReference.id
                println("Debt added successfully with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error adding debt: $e")
                onFailure(e)
            }
    }

    /**
     * Real-time listener for the user's debt records.
     */
    fun getDebtsRealtime(
        onDebtsLoaded: (List<Debt>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("debts")
            .orderBy("dueDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for debts: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val debtsList = snapshots?.map { doc ->
                    doc.toObject(Debt::class.java).apply { id = doc.id }
                } ?: emptyList()
                onDebtsLoaded(debtsList)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Calculates the sum of all unpaid debts in real-time.
     */
    fun getTotalUnpaidDebtsRealtime(
        onTotalDebtLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("debts")
            .whereEqualTo("isPaid", false)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for total unpaid debts: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf {
                    val amount = it.getDouble("amount") ?: 0.0
                    val paid = it.getDouble("amountPaid") ?: 0.0
                    amount - paid
                } ?: 0.0
                onTotalDebtLoaded(total)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Updates an existing debt document.
     */
    fun updateDebt(debt: Debt, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        if (debt.id.isEmpty()) {
            onFailure(Exception("Debt ID not provided for update."))
            return
        }

        val updates = hashMapOf<String, Any>(
            "amount" to debt.amount,
            "description" to debt.description,
            "creditor" to debt.creditor,
            "dueDate" to debt.dueDate,
            "isPaid" to debt.isPaid,
            "amountPaid" to debt.amountPaid
        )

        db.collection("users").document(userId)
            .collection("debts").document(debt.id)
            .update(updates)
            .addOnSuccessListener {
                println("Debt updated successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error updating debt: $e")
                onFailure(e)
            }
    }

    /**
     * Makes a payment towards a debt, updating the amountPaid field.
     * Automatically marks the debt as paid if amountPaid reaches or exceeds the total amount.
     */
    fun makeDebtPayment(
        debtId: String,
        paymentAmount: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        val debtRef = db.collection("users").document(userId)
            .collection("debts").document(debtId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(debtRef)
            val currentPaid = snapshot.getDouble("amountPaid") ?: 0.0
            val totalAmount = snapshot.getDouble("amount") ?: 0.0

            val newPaid = currentPaid + paymentAmount
            val isPaid = newPaid >= totalAmount

            transaction.update(debtRef, mapOf(
                "amountPaid" to newPaid,
                "isPaid" to isPaid
            ))
        }.addOnSuccessListener {
            println("Payment recorded successfully.")
            onSuccess()
        }.addOnFailureListener { e ->
            println("Error recording payment: $e")
            onFailure(e)
        }
    }

    /**
     * Deletes a specific debt document.
     */
    fun deleteDebt(debtId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        db.collection("users").document(userId)
            .collection("debts").document(debtId)
            .delete()
            .addOnSuccessListener {
                println("Debt deleted successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error deleting debt: $e")
                onFailure(e)
            }
    }

// ============================================================================================
// REGION: SAVINGS GOAL OPERATIONS
// ============================================================================================

    /**
     * Persists a new [SavingsGoal] record to the specific user's Firestore sub-collection.
     */
    fun addSavingsGoal(goal: SavingsGoal, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        val goalData = hashMapOf(
            "name" to goal.name,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "deadline" to goal.deadline,
            "isCompleted" to goal.isCompleted,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("savingsGoals")
            .add(goalData)
            .addOnSuccessListener { documentReference ->
                goal.id = documentReference.id
                println("Savings goal added successfully with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error adding savings goal: $e")
                onFailure(e)
            }
    }

    /**
     * Real-time listener for the user's savings goals.
     */
    fun getSavingsGoalsRealtime(
        onGoalsLoaded: (List<SavingsGoal>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("savingsGoals")
            .orderBy("deadline", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for savings goals: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val goalsList = snapshots?.map { doc ->
                    doc.toObject(SavingsGoal::class.java).apply { id = doc.id }
                } ?: emptyList()
                onGoalsLoaded(goalsList)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Calculates the total amount saved across all active goals in real-time.
     */
    fun getTotalSavedAmountRealtime(
        onTotalSavedLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("savingsGoals")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for total saved amount: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("currentAmount") ?: 0.0 } ?: 0.0
                onTotalSavedLoaded(total)
            }
    } ?: run { onFailure(Exception("User not authenticated.")) ; null }

    /**
     * Updates an existing savings goal document.
     */
    fun updateSavingsGoal(goal: SavingsGoal, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        if (goal.id.isEmpty()) {
            onFailure(Exception("Savings goal ID not provided for update."))
            return
        }

        val updates = hashMapOf<String, Any>(
            "name" to goal.name,
            "targetAmount" to goal.targetAmount,
            "currentAmount" to goal.currentAmount,
            "deadline" to goal.deadline,
            "isCompleted" to goal.isCompleted
        )

        db.collection("users").document(userId)
            .collection("savingsGoals").document(goal.id)
            .update(updates)
            .addOnSuccessListener {
                println("Savings goal updated successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error updating savings goal: $e")
                onFailure(e)
            }
    }

    /**
     * Adds money to a savings goal, updating the currentAmount field.
     * Automatically marks the goal as completed if currentAmount reaches or exceeds the targetAmount.
     */
    fun addToSavingsGoal(
        goalId: String,
        amountToAdd: Double,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        val goalRef = db.collection("users").document(userId)
            .collection("savingsGoals").document(goalId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(goalRef)
            val currentAmount = snapshot.getDouble("currentAmount") ?: 0.0
            val targetAmount = snapshot.getDouble("targetAmount") ?: 0.0

            val newAmount = currentAmount + amountToAdd
            val isCompleted = newAmount >= targetAmount

            transaction.update(goalRef, mapOf(
                "currentAmount" to newAmount,
                "isCompleted" to isCompleted
            ))
        }.addOnSuccessListener {
            println("Amount added to savings goal successfully.")
            onSuccess()
        }.addOnFailureListener { e ->
            println("Error adding to savings goal: $e")
            onFailure(e)
        }
    }

    /**
     * Deletes a specific savings goal document.
     */
    fun deleteSavingsGoal(goalId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("User not authenticated."))

        db.collection("users").document(userId)
            .collection("savingsGoals").document(goalId)
            .delete()
            .addOnSuccessListener {
                println("Savings goal deleted successfully.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error deleting savings goal: $e")
                onFailure(e)
            }
    }

}