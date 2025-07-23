package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.view.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.Chat

data class PredictionUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hola, soy tu asistente de predicciones financieras. ¿En qué puedo ayudarte?", false)
    ),
    val errorMessage: String? = null
)

class PredictionViewModel : ViewModel() {
    private val GEMINI_API_KEY = "AIzaSyBmPWQsscLDqGl-vsV38VKrWZvhkexu7z0"
    private val _uiState = MutableStateFlow(PredictionUiState())
    val uiState: StateFlow<PredictionUiState> = _uiState

    private lateinit var generativeModel: GenerativeModel
    private lateinit var chat: Chat

    init {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY
        )
        chat = generativeModel.startChat()
    }

    /**
     * Envía un mensaje al chat y procesa la respuesta
     * @param message Texto del mensaje del usuario
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + ChatMessage(message, true),
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val response = generateResponse(message)

                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage(response, false),
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error al obtener respuesta de la IA: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Genera una respuesta usando la API de Gemini y mantiene el contexto del chat.
     */
    private suspend fun generateResponse(userMessage: String): String {
        return try {
            val response = chat.sendMessage(userMessage)
            response.text ?: "Lo siento, no pude generar una respuesta en este momento."
        } catch (e: Exception) {
            "Error al comunicarse con la IA: ${e.localizedMessage}. Por favor, inténtalo de nuevo."
        }
    }

    /**
     * Reinicia los mensajes de error
     */
    fun resetErrorMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }
}