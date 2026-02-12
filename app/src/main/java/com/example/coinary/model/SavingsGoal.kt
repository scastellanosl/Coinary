package com.example.coinary.model

import com.google.firebase.Timestamp

data class SavingsGoal(
    var id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Timestamp = Timestamp.now(),
    val isCompleted: Boolean = false
) {
    /**
     * Calculates the remaining amount needed to reach the goal.
     */
    fun remainingAmount(): Double = targetAmount - currentAmount

    /**
     * Calculates the progress percentage towards the goal.
     */
    fun progressPercentage(): Float =
        if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toFloat() else 0f
}