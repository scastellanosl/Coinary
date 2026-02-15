package com.example.coinary.utils

import android.content.Context
import com.example.coinary.data.ReminderItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * ReminderStorage: A utility object dedicated to the local persistence of ReminderItem objects.
 * Uses SharedPreferences as the storage engine and GSON for JSON serialization/deserialization.
 */
object ReminderStorage {

    private const val PREFS_NAME = "CoinaryReminders"
    private const val REMINDERS_KEY = "remindersList"
    private val gson = Gson()

    /**
     * Persists a complete list of [ReminderItem] into SharedPreferences.
     * @param context Android context required to access SharedPreferences.
     * @param reminders The list of reminder objects to be serialized and saved.
     */
    fun saveReminders(context: Context, reminders: List<ReminderItem>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val json = gson.toJson(reminders)
        editor.putString(REMINDERS_KEY, json)
        editor.apply()
    }

    /**
     * Retrieves the stored list of [ReminderItem] from SharedPreferences.
     * @param context Android context required to access SharedPreferences.
     * @return A list of stored reminders, or an empty list if no data is found.
     */
    fun loadReminders(context: Context): List<ReminderItem> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(REMINDERS_KEY, null)

        return if (json == null) {
            emptyList()
        } else {
            val type = object : TypeToken<List<ReminderItem>>() {}.type
            gson.fromJson(json, type)
        }
    }

    /**
     * Appends a new [ReminderItem] to the existing local collection.
     * @param context Android context required for storage operations.
     * @param newReminder The reminder object to be added.
     */
    fun addReminder(context: Context, newReminder: ReminderItem) {
        val currentReminders = loadReminders(context).toMutableList()
        currentReminders.add(newReminder)
        saveReminders(context, currentReminders)
    }

    /**
     * Removes a specific reminder from the local collection based on its unique ID.
     * @param context Android context required for storage operations.
     * @param reminderId The unique long identifier of the reminder to be deleted.
     */
    fun removeReminder(context: Context, reminderId: Long) {
        val currentReminders = loadReminders(context).toMutableList()
        val updatedReminders = currentReminders.filter { it.id != reminderId }
        saveReminders(context, updatedReminders)
    }
}