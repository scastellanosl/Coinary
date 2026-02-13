package com.example.coinary.model

import com.google.firebase.Timestamp

/**
 * Data class representing an Expense movement in Coinary.
 * This structure maps directly to a Firestore document.
 */
data class Expense(
    var id: String = "",
    var amount: Double = 0.0,
    var description: String = "",
    var category: String = "",
    var date: Timestamp = Timestamp.now(),
    var isAntExpense: Boolean = false
)