package com.example.coinary.model

import com.google.firebase.Timestamp

data class Debt(
    var id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val creditor: String = "",
    val dueDate: Timestamp = Timestamp.now(),
    val isPaid: Boolean = false,
    val amountPaid: Double = 0.0
) {
    /**
     * Calculates the remaining balance to be paid.
     */
    fun remainingBalance(): Double = amount - amountPaid

    /**
     * Calculates the progress percentage of debt payment.
     */
    fun progressPercentage(): Float =
        if (amount > 0) ((amountPaid / amount) * 100).toFloat() else 0f
}