package com.example.coinary.model

import java.util.Date

/**
 * Movement: Generic data model for financial transactions.
 * Often used as a unified structure for displaying both Incomes and Expenses in history lists.
 */
data class Movement(
    val id: String = "",
    val tipo: String = "Ingreso",
    val categoria: String = "",
    val monto: Double = 0.0,
    val descripcion: String = "",
    val fecha: Date = Date()
)