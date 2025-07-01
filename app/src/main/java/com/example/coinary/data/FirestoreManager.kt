package com.example.coinary.data

import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
    fun getIncomesRealtime(onIncomesLoaded: (List<Income>) -> Unit, onFailure: (Exception) -> Unit) =
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
    fun getExpensesRealtime(onExpensesLoaded: (List<Expense>) -> Unit, onFailure: (Exception) -> Unit) =
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
    fun getTotalIncomesRealtime(onTotalIncomeLoaded: (Double) -> Unit, onFailure: (Exception) -> Unit) =
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
    fun getTotalExpensesRealtime(onTotalExpenseLoaded: (Double) -> Unit, onFailure: (Exception) -> Unit) =
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
}
