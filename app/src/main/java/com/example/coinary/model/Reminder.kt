package com.example.coinary.model

import java.util.Date

/**
 * Reminder: Data model representing a scheduled financial alert.
 * This class is used to track future obligations or planned movements
 * that the user needs to be notified about.
 */
data class Reminder(
    val id: String = "",
    val titulo: String = "",
    val categoria: String = "",
    val monto: Double = 0.0,
    val descripcion: String = "",
    val fecha: Date = Date()
)