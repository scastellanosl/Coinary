package com.example.coinary.data

/**
 * ReminderItem: Data model representing a scheduled financial reminder.
 * Used for managing local or push notifications within the app.
 */
data class ReminderItem(
    /** Unique identifier for the reminder (useful for canceling specific alarms) */
    val id: Long,

    /** The main title shown in the notification (e.g., "Payment Due") */
    val title: String,

    /** Detailed context of the reminder (e.g., "Electricity bill expires today") */
    val message: String,

    /** * Scheduled date and time for the reminder.
     * Expected format: "dd/MM/yyyy HH:mm"
     */
    val dateTime: String
)