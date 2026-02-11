package com.example.coinary.data

import com.example.coinary.model.Expense
import com.example.coinary.model.Income
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
}