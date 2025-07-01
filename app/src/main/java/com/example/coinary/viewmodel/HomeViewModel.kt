package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.data.FirestoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val firestoreManager: FirestoreManager = FirestoreManager()
) : ViewModel() {

    private val _totalIncome = MutableStateFlow(0.0)
    val totalIncome: StateFlow<Double> = _totalIncome

    private val _totalExpenses = MutableStateFlow(0.0)
    val totalExpenses: StateFlow<Double> = _totalExpenses

    init {
        // Iniciar la escucha de totales cuando el ViewModel se crea
        fetchTotals()
    }

    private fun fetchTotals() {
        viewModelScope.launch {
            // Escuchar el total de ingresos en tiempo real
            firestoreManager.getTotalIncomesRealtime(
                onTotalIncomeLoaded = { total ->
                    _totalIncome.value = total
                },
                onFailure = { e ->
                    println("Error fetching total income: ${e.message}")
                    // Aquí podrías manejar el error, por ejemplo, mostrando un Toast o un mensaje en la UI
                }
            )

            // Escuchar el total de gastos en tiempo real
            firestoreManager.getTotalExpensesRealtime(
                onTotalExpenseLoaded = { total ->
                    _totalExpenses.value = total
                },
                onFailure = { e ->
                    println("Error fetching total expenses: ${e.message}")
                    // Aquí podrías manejar el error
                }
            )
        }
    }
}
