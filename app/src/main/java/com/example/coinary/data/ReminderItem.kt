package com.example.coinary.data


data class ReminderItem(
    val id: Long,
    val title: String,
    val message: String,
    val dateTime: String // "dd/MM/yyyy HH:mm"
)
