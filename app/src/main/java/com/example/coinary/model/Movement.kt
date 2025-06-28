package com.example.coinary.model

import java.util.Date

data class Movement(
    val id: String = "",
    val tipo: String = "Ingreso",
    val categoria: String = "",
    val monto: Double = 0.0,
    val descripcion: String = "",
    val fecha: Date = Date()
)
