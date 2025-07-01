package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import com.example.coinary.model.Expense
import com.example.coinary.model.Income
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

// Clase de datos para el estado de la UI
data class MovementUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class MovementViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovementUiState())
    val uiState: StateFlow<MovementUiState> = _uiState

    /**
     * Guarda un nuevo ingreso en Firestore.
     * @param amount Cantidad del ingreso.
     * @param description Descripción del ingreso.
     * @param category Categoría del ingreso.
     * @param date Fecha del ingreso.
     */
    fun saveIncome(amount: Double, description: String, category: String, date: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true, successMessage = null, errorMessage = null)
        viewModelScope.launch {
            val income = Income(
                amount = amount,
                description = description,
                category = category,
                date = Timestamp(date)
            )
            firestoreManager.addIncome(
                income,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Ingreso guardado exitosamente."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al guardar ingreso: ${e.message}"
                    )
                }
            )
        }
    }

    /**
     * Guarda un nuevo gasto en Firestore.
     * @param amount Cantidad del gasto.
     * @param description Descripción del gasto.
     * @param category Categoría del gasto.
     * @param date Fecha del gasto.
     */
    fun saveExpense(amount: Double, description: String, category: String, date: Date) {
        _uiState.value = _uiState.value.copy(isLoading = true, successMessage = null, errorMessage = null)
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                description = description,
                category = category,
                date = Timestamp(date)
            )
            firestoreManager.addExpense(
                expense,
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Gasto guardado exitosamente."
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Error al guardar gasto: ${e.message}"
                    )
                }
            )
        }
    }

    /**
     * Reinicia los mensajes de éxito/error después de que se hayan mostrado.
     */
    fun resetMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }
}
