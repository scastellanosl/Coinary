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
 * FirestoreManager: The core data engine for Coinary.
 * Handles all CRUD operations and real-time data synchronization with Firebase Firestore.
 */
class FirestoreManager {

    private val db: FirebaseFirestore = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Helper to retrieve the current authenticated user's ID.
     */
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ============================================================================================
    // REGION: INCOME OPERATIONS
    // ============================================================================================

    /**
     * Adds a new income record to Firestore.
     */
    fun addIncome(income: Income, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: User not authenticated."))

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
            .addOnSuccessListener { docRef ->
                income.id = docRef.id
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Real-time stream of all user incomes sorted by date.
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
                    onFailure(e)
                    return@addSnapshotListener
                }
                val incomesList = snapshots?.map { doc ->
                    doc.toObject(Income::class.java).apply { id = doc.id }
                } ?: emptyList()
                onIncomesLoaded(incomesList)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Real-time stream of the all-time total income sum.
     */
    fun getTotalIncomesRealtime(
        onTotalIncomeLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("incomes")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onTotalIncomeLoaded(total)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Sums incomes within a specific date range (Day, Week, Month, etc.).
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
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onResult(total)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Updates an existing income record.
     */
    fun updateIncome(income: Income, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        if (income.id.isEmpty()) return onFailure(Exception("UPDATE_ERROR: Missing Document ID."))

        val updates = hashMapOf<String, Any>(
            "amount" to income.amount,
            "description" to income.description,
            "category" to income.category,
            "date" to income.date
        )

        db.collection("users").document(userId)
            .collection("incomes").document(income.id)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Deletes a specific income record.
     */
    fun deleteIncome(incomeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        db.collection("users").document(userId)
            .collection("incomes").document(incomeId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // ============================================================================================
    // REGION: EXPENSE OPERATIONS
    // ============================================================================================

    /**
     * Persists a new expense, including the automatic "Ant Expense" detection flag.
     */
    fun addExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: User not authenticated."))

        val expenseData = hashMapOf(
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "date" to expense.date,
            "isAntExpense" to expense.isAntExpense,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("expenses")
            .add(expenseData)
            .addOnSuccessListener { docRef ->
                expense.id = docRef.id
                onSuccess()
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Intelligent detection: Counts category occurrences in the last 7 days.
     */
    fun getRecentExpensesCount(category: String, onResult: (Int) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))

        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val oneWeekAgo = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereEqualTo("category", category)
            .whereGreaterThanOrEqualTo("date", oneWeekAgo)
            .get()
            .addOnSuccessListener { snapshot -> onResult(snapshot.size()) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Streams all expenses flagged as "Ant Expenses".
     */
    fun getAntExpensesRealtime(
        onExpensesLoaded: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("expenses")
            .whereEqualTo("isAntExpense", true)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onFailure(e)
                    return@addSnapshotListener
                }
                val list = snapshots?.map { doc ->
                    doc.toObject(Expense::class.java).apply { id = doc.id }
                } ?: emptyList()
                onExpensesLoaded(list)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Real-time stream of the all-time total expense sum.
     */
    fun getTotalExpensesRealtime(
        onTotalExpenseLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        db.collection("users").document(userId)
            .collection("expenses")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onTotalExpenseLoaded(total)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Sums expenses within a specific date range.
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
                    onFailure(e)
                    return@addSnapshotListener
                }
                val total = snapshots?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                onResult(total)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Updates an existing expense record.
     */
    fun updateExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        if (expense.id.isEmpty()) return onFailure(Exception("UPDATE_ERROR: Missing Document ID."))

        val updates = hashMapOf<String, Any>(
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "date" to expense.date
        )

        db.collection("users").document(userId)
            .collection("expenses").document(expense.id)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    /**
     * Deletes a specific expense record.
     */
    fun deleteExpense(expenseId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        db.collection("users").document(userId)
            .collection("expenses").document(expenseId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // ============================================================================================
    // REGION: ANALYTICS & STATS
    // ============================================================================================

    /**
     * Aggregates current weekly expenses by category.
     */
    fun getWeeklyExpensesByCategoryRealtime(
        onCategorizedExpensesLoaded: (Map<String, Double>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val calendar = Calendar.getInstance(Locale.getDefault()).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = Timestamp(calendar.time)
        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59)
        val endOfWeek = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startOfWeek)
            .whereLessThanOrEqualTo("date", endOfWeek)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { onFailure(e); return@addSnapshotListener }
                val categorized = mutableMapOf<String, Double>()
                snapshots?.forEach { doc ->
                    val cat = doc.getString("category") ?: "Other"
                    val amt = doc.getDouble("amount") ?: 0.0
                    categorized[cat] = (categorized[cat] ?: 0.0) + amt
                }
                onCategorizedExpensesLoaded(categorized)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    /**
     * Aggregates expenses by category within a custom date range.
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
                if (e != null) { onFailure(e); return@addSnapshotListener }
                val categorized = mutableMapOf<String, Double>()
                snapshots?.forEach { doc ->
                    val cat = doc.getString("category") ?: "Other"
                    val amt = doc.getDouble("amount") ?: 0.0
                    categorized[cat] = (categorized[cat] ?: 0.0) + amt
                }
                onCategorizedExpensesLoaded(categorized)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    // ============================================================================================
    // REGION: MONTHLY FETCH (USED BY PDF GENERATOR)
    // ============================================================================================

    fun getMonthlyIncomesRealtime(
        month: Int, year: Int,
        onIncomesLoaded: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val (start, end) = calculateMonthRange(month, year)
        db.collection("users").document(userId).collection("incomes")
            .whereGreaterThanOrEqualTo("date", start).whereLessThanOrEqualTo("date", end)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { onFailure(e); return@addSnapshotListener }
                val list = snapshots?.map { it.toObject(Income::class.java).apply { id = it.id } } ?: emptyList()
                onIncomesLoaded(list)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    fun getMonthlyExpensesRealtime(
        month: Int, year: Int,
        onExpensesLoaded: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val (start, end) = calculateMonthRange(month, year)
        db.collection("users").document(userId).collection("expenses")
            .whereGreaterThanOrEqualTo("date", start).whereLessThanOrEqualTo("date", end)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) { onFailure(e); return@addSnapshotListener }
                val list = snapshots?.map { it.toObject(Expense::class.java).apply { id = it.id } } ?: emptyList()
                onExpensesLoaded(list)
            }
    } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    fun getMonthlyIncomesOnce(month: Int, year: Int, onSuccess: (List<Income>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val (start, end) = calculateMonthRange(month, year)
        db.collection("users").document(userId).collection("incomes")
            .whereGreaterThanOrEqualTo("date", start).whereLessThanOrEqualTo("date", end).get()
            .addOnSuccessListener { snap -> onSuccess(snap.map { it.toObject(Income::class.java).apply { id = it.id } }) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getMonthlyExpensesOnce(month: Int, year: Int, onSuccess: (List<Expense>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val (start, end) = calculateMonthRange(month, year)
        db.collection("users").document(userId).collection("expenses")
            .whereGreaterThanOrEqualTo("date", start).whereLessThanOrEqualTo("date", end).get()
            .addOnSuccessListener { snap -> onSuccess(snap.map { it.toObject(Expense::class.java).apply { id = it.id } }) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    private fun calculateMonthRange(month: Int, year: Int): Pair<Timestamp, Timestamp> {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = Timestamp(cal.time)
        cal.add(Calendar.MONTH, 1); cal.add(Calendar.MILLISECOND, -1)
        return Pair(start, Timestamp(cal.time))
    }

    // ============================================================================================
    // REGION: DEBT OPERATIONS
    // ============================================================================================

    fun addDebt(debt: Debt, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val data = hashMapOf(
            "amount" to debt.amount, "description" to debt.description, "creditor" to debt.creditor,
            "dueDate" to debt.dueDate, "isPaid" to debt.isPaid, "amountPaid" to debt.amountPaid, "createdAt" to Timestamp.now()
        )
        db.collection("users").document(userId).collection("debts").add(data)
            .addOnSuccessListener { doc -> debt.id = doc.id; onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getDebtsRealtime(onDebtsLoaded: (List<Debt>) -> Unit, onFailure: (Exception) -> Unit) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId).collection("debts")
                .orderBy("dueDate", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) { onFailure(e); return@addSnapshotListener }
                    val list = snapshots?.map { it.toObject(Debt::class.java).apply { id = it.id } } ?: emptyList()
                    onDebtsLoaded(list)
                }
        } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    fun getTotalUnpaidDebtsRealtime(onTotalDebtLoaded: (Double) -> Unit, onFailure: (Exception) -> Unit) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId).collection("debts")
                .whereEqualTo("isPaid", false)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) { onFailure(e); return@addSnapshotListener }
                    val total = snapshots?.sumOf { (it.getDouble("amount") ?: 0.0) - (it.getDouble("amountPaid") ?: 0.0) } ?: 0.0
                    onTotalDebtLoaded(total)
                }
        } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    fun updateDebt(debt: Debt, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val updates = hashMapOf<String, Any>(
            "amount" to debt.amount, "description" to debt.description, "creditor" to debt.creditor,
            "dueDate" to debt.dueDate, "isPaid" to debt.isPaid, "amountPaid" to debt.amountPaid
        )
        db.collection("users").document(userId).collection("debts").document(debt.id).update(updates)
            .addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e) }
    }

    fun makeDebtPayment(debtId: String, paymentAmount: Double, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val ref = db.collection("users").document(userId).collection("debts").document(debtId)
        db.runTransaction { transaction ->
            val snap = transaction.get(ref)
            val newPaid = (snap.getDouble("amountPaid") ?: 0.0) + paymentAmount
            val total = snap.getDouble("amount") ?: 0.0
            transaction.update(ref, mapOf("amountPaid" to newPaid, "isPaid" to (newPaid >= total)))
        }.addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteDebt(debtId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        db.collection("users").document(userId).collection("debts").document(debtId).delete()
            .addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e) }
    }

    // ============================================================================================
    // REGION: SAVINGS GOAL OPERATIONS
    // ============================================================================================

    fun addSavingsGoal(goal: SavingsGoal, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val data = hashMapOf(
            "name" to goal.name, "targetAmount" to goal.targetAmount, "currentAmount" to goal.currentAmount,
            "deadline" to goal.deadline, "isCompleted" to goal.isCompleted, "createdAt" to Timestamp.now()
        )
        db.collection("users").document(userId).collection("savingsGoals").add(data)
            .addOnSuccessListener { doc -> goal.id = doc.id; onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getSavingsGoalsRealtime(onGoalsLoaded: (List<SavingsGoal>) -> Unit, onFailure: (Exception) -> Unit) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId).collection("savingsGoals")
                .orderBy("deadline", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) { onFailure(e); return@addSnapshotListener }
                    val list = snapshots?.map { it.toObject(SavingsGoal::class.java).apply { id = it.id } } ?: emptyList()
                    onGoalsLoaded(list)
                }
        } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    fun getTotalSavedAmountRealtime(onTotalSavedLoaded: (Double) -> Unit, onFailure: (Exception) -> Unit) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId).collection("savingsGoals")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) { onFailure(e); return@addSnapshotListener }
                    val total = snapshots?.sumOf { it.getDouble("currentAmount") ?: 0.0 } ?: 0.0
                    onTotalSavedLoaded(total)
                }
        } ?: run { onFailure(Exception("AUTH_ERROR: Session expired.")); null }

    fun updateSavingsGoal(goal: SavingsGoal, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val updates = hashMapOf<String, Any>(
            "name" to goal.name, "targetAmount" to goal.targetAmount, "currentAmount" to goal.currentAmount,
            "deadline" to goal.deadline, "isCompleted" to goal.isCompleted
        )
        db.collection("users").document(userId).collection("savingsGoals").document(goal.id).update(updates)
            .addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e) }
    }

    fun addToSavingsGoal(goalId: String, amountToAdd: Double, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        val ref = db.collection("users").document(userId).collection("savingsGoals").document(goalId)
        db.runTransaction { transaction ->
            val snap = transaction.get(ref)
            val newAmount = (snap.getDouble("currentAmount") ?: 0.0) + amountToAdd
            val target = snap.getDouble("targetAmount") ?: 0.0
            transaction.update(ref, mapOf("currentAmount" to newAmount, "isCompleted" to (newAmount >= target)))
        }.addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e) }
    }

    fun deleteSavingsGoal(goalId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        db.collection("users").document(userId).collection("savingsGoals").document(goalId).delete()
            .addOnSuccessListener { onSuccess() }.addOnFailureListener { e -> onFailure(e) }
    }

    // ============================================================================================
    // REGION: REPORT GENERATION HELPERS
    // ============================================================================================

    fun getAllSavingsGoals(onSuccess: (List<SavingsGoal>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        db.collection("users").document(userId).collection("savingsGoals").get()
            .addOnSuccessListener { snap -> onSuccess(snap.map { it.toObject(SavingsGoal::class.java).apply { id = it.id } }) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getAllDebts(onSuccess: (List<Debt>) -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId() ?: return onFailure(Exception("AUTH_ERROR: Session expired."))
        db.collection("users").document(userId).collection("debts").get()
            .addOnSuccessListener { snap -> onSuccess(snap.map { it.toObject(Debt::class.java).apply { id = it.id } }) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getIncomesByMonth(month: Int, year: Int, onSuccess: (List<Income>) -> Unit, onFailure: (Exception) -> Unit) = getMonthlyIncomesOnce(month, year, onSuccess, onFailure)

    fun getExpensesByMonth(month: Int, year: Int, onSuccess: (List<Expense>) -> Unit, onFailure: (Exception) -> Unit) = getMonthlyExpensesOnce(month, year, onSuccess, onFailure)
}