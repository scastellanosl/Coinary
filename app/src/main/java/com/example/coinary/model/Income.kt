package com.example.coinary.model

import com.google.firebase.Timestamp

/**
 * Data class representing an Income movement in Coinary.
 * This structure maps directly to a Firestore document.
 */
data class Income(
    var id: String = "", // Firestore document ID, will be assigned after saving
    val amount: Double = 0.0,
    val description: String = "",
    val category: String = "",
    val date: Timestamp = Timestamp.now(), // Date of the income
    val createdAt: Timestamp = Timestamp.now() // Timestamp when the record was created in Firestore
)
