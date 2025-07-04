package com.example.coinary.utils

import android.content.Context
import com.example.coinary.data.ReminderItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ReminderStorage {

    private const val PREFS_NAME = "CoinaryReminders"
    private const val REMINDERS_KEY = "remindersList"
    private val gson = Gson()
    /**
     * Guarda una lista de ReminderItem en SharedPreferences.
     */
    fun saveReminders(context: Context, reminders: List<ReminderItem>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val json = gson.toJson(reminders) // Convierte la lista de objetos a una cadena JSON
        editor.putString(REMINDERS_KEY, json) // Guarda la cadena JSON
        editor.apply() // Aplica los cambios de forma asíncrona
    }

    /**
     * Carga una lista de ReminderItem desde SharedPreferences.
     * Retorna una lista vacía si no hay recordatorios guardados.
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
     * Añade un nuevo recordatorio a la lista existente y la guarda.
     */
    fun addReminder(context: Context, newReminder: ReminderItem) {
        val currentReminders = loadReminders(context).toMutableList()
        currentReminders.add(newReminder)
        saveReminders(context, currentReminders)
    }

    /**
     * Elimina un recordatorio por su ID de la lista y la guarda.
     */
    fun removeReminder(context: Context, reminderId: Long) {
        val currentReminders = loadReminders(context).toMutableList()
        val updatedReminders = currentReminders.filter { it.id != reminderId }
        saveReminders(context, updatedReminders)
    }
}