package com.example.coinary.model

import java.util.Date

data class Reminder(
    val id: String = "",
    val titulo: String = "",
    val categoria: String = "",
    val monto: Double = 0.0,
    val descripcion: String = "",
    val fecha: Date = Date()
)
