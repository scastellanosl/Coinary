package com.example.coinary.data

import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Calendar
import java.util.Locale

/**
 * Manages all interactions with Firebase Firestore for the Coinary application.
 * Handles adding, retrieving, updating, and deleting income and expense data.
 */
class FirestoreManager {

    private val db = Firebase.firestore // Firestore database instance
    private val auth = FirebaseAuth.getInstance() // Firebase Authentication instance

    /**
     * Retrieves the current authenticated user's UID.
     * @return The UID of the current user, or null if no user is authenticated.
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Adds a new income record for the current user to Firestore.
     * @param income The Income object to add.
     * @param onSuccess Callback to execute if the operation is successful.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     */
    fun addIncome(income: Income, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }

        // Create a map with the income data to be stored in Firestore
        val incomeData = hashMapOf(
            "amount" to income.amount,
            "description" to income.description,
            "category" to income.category,
            "date" to income.date,
            "createdAt" to Timestamp.now() // Ensure createdAt is set at the time of creation
        )

        // Add the document to the 'incomes' subcollection of the current user
        db.collection("users").document(userId)
            .collection("incomes")
            .add(incomeData) // add() automatically generates an ID for the document
            .addOnSuccessListener { documentReference ->
                // Optional: You can save the generated ID back to your Income object if needed
                income.id = documentReference.id
                println("Income added with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error adding income: $e")
                onFailure(e)
            }
    }

    /**
     * Adds a new expense record for the current user to Firestore.
     * @param expense The Expense object to add.
     * @param onSuccess Callback to execute if the operation is successful.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     */
    fun addExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }

        // Create a map with the expense data to be stored in Firestore
        val expenseData = hashMapOf(
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "date" to expense.date,
            "createdAt" to Timestamp.now() // Ensure createdAt is set at the time of creation
        )

        // Add the document to the 'expenses' subcollection of the current user
        db.collection("users").document(userId)
            .collection("expenses")
            .add(expenseData) // add() automatically generates an ID for the document
            .addOnSuccessListener { documentReference ->
                // Optional: You can save the generated ID back to your Expense object if needed
                expense.id = documentReference.id
                println("Expense added with ID: ${documentReference.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error adding expense: $e")
                onFailure(e)
            }
    }

    /**
     * Retrieves all income records for the current user in real-time.
     * @param onIncomesLoaded Callback that is invoked with the list of incomes.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     * @return A ListenerRegistration that can be used to stop listening for updates.
     */
    fun getIncomesRealtime(
        onIncomesLoaded: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId)
                .collection("incomes")
                .orderBy("date", Query.Direction.DESCENDING) // Order by date in descending order
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        println("Error listening for incomes: $e")
                        onFailure(e)
                        return@addSnapshotListener
                    }

                    val incomesList = mutableListOf<Income>()
                    if (snapshots != null) {
                        for (doc in snapshots) {
                            val income = doc.toObject(Income::class.java)
                            income.id = doc.id // Assign the document ID to the object
                            incomesList.add(income)
                        }
                    }
                    onIncomesLoaded(incomesList)
                }
        } ?: run {
            onFailure(Exception("User not authenticated."))
            null // Return null if the user is not authenticated
        }

    /**
     * Retrieves all expense records for the current user in real-time.
     * @param onExpensesLoaded Callback that is invoked with the list of expenses.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     * @return A ListenerRegistration that can be used to stop listening for updates.
     */
    fun getExpensesRealtime(
        onExpensesLoaded: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId)
                .collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING) // Order by date in descending order
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        println("Error listening for expenses: $e")
                        onFailure(e)
                        return@addSnapshotListener
                    }

                    val expensesList = mutableListOf<Expense>()
                    if (snapshots != null) {
                        for (doc in snapshots) {
                            val expense = doc.toObject(Expense::class.java)
                            expense.id = doc.id // Assign the document ID to the object
                            expensesList.add(expense)
                        }
                    }
                    onExpensesLoaded(expensesList)
                }
        } ?: run {
            onFailure(Exception("User not authenticated."))
            null // Return null if the user is not authenticated
        }

    /**
     * Calculates and provides the total sum of all income amounts for the current user in real-time.
     * @param onTotalIncomeLoaded Callback that is invoked with the total income amount.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     * @return A ListenerRegistration that can be used to stop listening for updates.
     */
    fun getTotalIncomesRealtime(
        onTotalIncomeLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId)
                .collection("incomes")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        println("Error listening for total incomes: $e")
                        onFailure(e)
                        return@addSnapshotListener
                    }

                    var total = 0.0
                    if (snapshots != null) {
                        for (doc in snapshots) {
                            val amount = doc.getDouble("amount") ?: 0.0
                            total += amount
                        }
                    }
                    onTotalIncomeLoaded(total)
                }
        } ?: run {
            onFailure(Exception("User not authenticated."))
            null
        }

    /**
     * Calculates and provides the total sum of all expense amounts for the current user in real-time.
     * @param onTotalExpenseLoaded Callback that is invoked with the total expense amount.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     * @return A ListenerRegistration that can be used to stop listening for updates.
     */
    fun getTotalExpensesRealtime(
        onTotalExpenseLoaded: (Double) -> Unit,
        onFailure: (Exception) -> Unit
    ) =
        getCurrentUserId()?.let { userId ->
            db.collection("users").document(userId)
                .collection("expenses")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        println("Error listening for total expenses: $e")
                        onFailure(e)
                        return@addSnapshotListener
                    }

                    var total = 0.0
                    if (snapshots != null) {
                        for (doc in snapshots) {
                            val amount = doc.getDouble("amount") ?: 0.0
                            total += amount
                        }
                    }
                    onTotalExpenseLoaded(total)
                }
        } ?: run {
            onFailure(Exception("User not authenticated."))
            null
        }

    /**
     * Retrieves expense amounts grouped by category for the current week in real-time.
     * The week is defined from Monday to Sunday.
     * @param onCategorizedExpensesLoaded Callback invoked with a map of category to total amount.
     * @param onFailure Callback invoked if an error occurs.
     * @return A ListenerRegistration to stop listening for updates.
     */
    fun getWeeklyExpensesByCategoryRealtime(
        onCategorizedExpensesLoaded: (Map<String, Double>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.firstDayOfWeek =
            Calendar.MONDAY // Establece el lunes como el primer día de la semana

        // Calcular el inicio de la semana (lunes a las 00:00:00)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = Timestamp(calendar.time)

        // Calcular el fin de la semana (domingo a las 23:59:59.999)
        calendar.add(Calendar.DATE, 6) // Avanza 6 días para llegar al domingo
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo(
                "date",
                startOfWeek
            ) // Filtra gastos desde el inicio de la semana
            .whereLessThanOrEqualTo(
                "date",
                endOfWeek
            )     // Filtra gastos hasta el fin de la semana
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for weekly categorized expenses: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }

                val categorizedExpenses = mutableMapOf<String, Double>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val category = doc.getString("category") ?: "Uncategorized"
                        val amount = doc.getDouble("amount") ?: 0.0
                        categorizedExpenses[category] =
                            (categorizedExpenses[category] ?: 0.0) + amount
                    }
                }
                onCategorizedExpensesLoaded(categorizedExpenses)
            }
    } ?: run {
        onFailure(Exception("User not authenticated."))
        null
    }

    /**
     * Retrieves all income records for a specific month and year in real-time.
     * @param month The month (1-12).
     * @param year The year.
     * @param onIncomesLoaded Callback invoked with the list of incomes.
     * @param onFailure Callback invoked if an error occurs.
     * @return A ListenerRegistration to stop listening for updates.
     */
    fun getMonthlyIncomesRealtime(
        month: Int,
        year: Int,
        onIncomesLoaded: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar.MONTH es 0-indexed
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1) // Avanza al siguiente mes
        calendar.add(Calendar.MILLISECOND, -1) // Resta 1ms para obtener el último ms del mes actual
        val endOfMonth = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("incomes")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for monthly incomes: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val incomesList = mutableListOf<Income>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val income = doc.toObject(Income::class.java)
                        income.id = doc.id
                        incomesList.add(income)
                    }
                }
                onIncomesLoaded(incomesList)
            }
    } ?: run {
        onFailure(Exception("User not authenticated."))
        null
    }

    /**
     * Retrieves all expense records for a specific month and year in real-time.
     * @param month The month (1-12).
     * @param year The year.
     * @param onExpensesLoaded Callback invoked with the list of expenses.
     * @param onFailure Callback invoked if an error occurs.
     * @return A ListenerRegistration to stop listening for updates.
     */
    fun getMonthlyExpensesRealtime(
        month: Int,
        year: Int,
        onExpensesLoaded: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) = getCurrentUserId()?.let { userId ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar.MONTH es 0-indexed
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1) // Avanza al siguiente mes
        calendar.add(Calendar.MILLISECOND, -1) // Resta 1ms para obtener el último ms del mes actual
        val endOfMonth = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Error listening for monthly expenses: $e")
                    onFailure(e)
                    return@addSnapshotListener
                }
                val expensesList = mutableListOf<Expense>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val expense = doc.toObject(Expense::class.java)
                        expense.id = doc.id
                        expensesList.add(expense)
                    }
                }
                onExpensesLoaded(expensesList)
            }
    } ?: run {
        onFailure(Exception("User not authenticated."))
        null
    }

    /**
     * Retrieves all income records for a specific month and year (one-time fetch).
     * @param month The month (1-12).
     * @param year The year.
     * @param onSuccess Callback invoked with the list of incomes.
     * @param onFailure Callback invoked if an error occurs.
     */
    fun getMonthlyIncomesOnce(
        month: Int,
        year: Int,
        onSuccess: (List<Income>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("incomes")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .get() // Use get() for one-time fetch
            .addOnSuccessListener { snapshots ->
                val incomesList = mutableListOf<Income>()
                for (doc in snapshots) {
                    val income = doc.toObject(Income::class.java)
                    income.id = doc.id
                    incomesList.add(income)
                }
                onSuccess(incomesList)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    /**
     * Retrieves all expense records for a specific month and year (one-time fetch).
     * @param month The month (1-12).
     * @param year The year.
     * @param onSuccess Callback invoked with the list of expenses.
     * @param onFailure Callback invoked if an error occurs.
     */
    fun getMonthlyExpensesOnce(
        month: Int,
        year: Int,
        onSuccess: (List<Expense>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = Timestamp(calendar.time)

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = Timestamp(calendar.time)

        db.collection("users").document(userId)
            .collection("expenses")
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThanOrEqualTo("date", endOfMonth)
            .get() // Use get() for one-time fetch
            .addOnSuccessListener { snapshots ->
                val expensesList = mutableListOf<Expense>()
                for (doc in snapshots) {
                    val expense = doc.toObject(Expense::class.java)
                    expense.id = doc.id
                    expensesList.add(expense)
                }
                onSuccess(expensesList)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }


    /**
     * Updates an existing income record in Firestore.
     * @param income The Income object with updated data (the ID must be present).
     * @param onSuccess Callback to execute if the operation is successful.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     */
    fun updateIncome(income: Income, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }
        if (income.id.isEmpty()) {
            onFailure(Exception("Income ID not provided for update."))
            return
        }

        val incomeRef = db.collection("users").document(userId)
            .collection("incomes").document(income.id)

        // Create a map with the fields to update
        val updates = hashMapOf<String, Any>(
            "amount" to income.amount,
            "description" to income.description,
            "category" to income.category,
            "date" to income.date
        )

        incomeRef.update(updates)
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
     * Updates an existing expense record in Firestore.
     * @param expense The Expense object with updated data (the ID must be present).
     * @param onSuccess Callback to execute if the operation is successful.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     */
    fun updateExpense(expense: Expense, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }
        if (expense.id.isEmpty()) {
            onFailure(Exception("Expense ID not provided for update."))
            return
        }

        val expenseRef = db.collection("users").document(userId)
            .collection("expenses").document(expense.id)

        // Create a map with the fields to update
        val updates = hashMapOf<String, Any>(
            "amount" to expense.amount,
            "description" to expense.description,
            "category" to expense.category,
            "date" to expense.date
        )

        expenseRef.update(updates)
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
     * Deletes an income record from Firestore.
     * @param incomeId The ID of the income to delete.
     * @param onSuccess Callback to execute if the operation is successful.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     */
    fun deleteIncome(incomeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }

        db.collection("users").document(userId)
            .collection("incomes").document(incomeId)
            .delete()
            .addOnSuccessListener {
                println("Ingreso eliminado correctamente.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error al eliminar ingreso: $e")
                onFailure(e)
            }
    }

    /**
     * Deletes an expense record from Firestore.
     * @param expenseId The ID of the expense to delete.
     * @param onSuccess Callback to execute if the operation is successful.
     * @param onFailure Callback to execute if the operation fails, providing the Exception.
     */
    fun deleteExpense(expenseId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            onFailure(Exception("User not authenticated."))
            return
        }

        db.collection("users").document(userId)
            .collection("expenses").document(expenseId)
            .delete()
            .addOnSuccessListener {
                println("Gasto eliminado correctamente.")
                onSuccess()
            }
            .addOnFailureListener { e ->
                println("Error al eliminar gasto: $e")
                onFailure(e)
            }
    }

    /**
     * Retrieves expense amounts grouped by category within a specific date range in real-time.
     * @param startDate The start Timestamp of the range.
     * @param endDate The end Timestamp of the range.
     * @param onCategorizedExpensesLoaded Callback invoked with a map of category to total amount.
     * @param onFailure Callback invoked if an error occurs.
     * @return A ListenerRegistration to stop listening for updates.
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
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val category = doc.getString("category") ?: "Uncategorized"
                        val amount = doc.getDouble("amount") ?: 0.0
                        // Sumar al acumulador de la categoría
                        categorizedExpenses[category] = (categorizedExpenses[category] ?: 0.0) + amount
                    }
                }
                onCategorizedExpensesLoaded(categorizedExpenses)
            }
    } ?: run {
        onFailure(Exception("User not authenticated."))
        null
    }
}