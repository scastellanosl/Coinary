package com.example.coinary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinary.view.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.Chat

data class RecommendationUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = listOf(
        ChatMessage("Hola, soy tu asistente de recomendaciones financieras. ¿En qué puedo ayudarte?", false)
    ),
    val errorMessage: String? = null
)

class RecommendationViewModel : ViewModel() {
    private val GEMINI_API_KEY = "AIzaSyBmPWQsscLDqGl-vsV38VKrWZvhkexu7z0"
    private val _uiState = MutableStateFlow(RecommendationUiState())
    val uiState: StateFlow<RecommendationUiState> = _uiState

    private lateinit var generativeModel: GenerativeModel
    private lateinit var chat: Chat

    init {
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = GEMINI_API_KEY
        )
        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") {
                    text("Actúa como un experto asesor financiero especializado en recomendaciones personalizadas.")
                },
                content(role = "model") {
                    text("Entendido. Soy tu asesor financiero especializado. Proporcionaré recomendaciones personalizadas basadas en tus necesidades. ¿En qué área necesitas asesoramiento hoy? (inversiones, ahorro, presupuesto, créditos, etc.)")
                }
            )
        )
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
                    errorMessage = "Error al obtener recomendaciones: ${e.localizedMessage}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Genera una respuesta usando la API de Gemini con enfoque en recomendaciones financieras
     */
    private suspend fun generateResponse(userMessage: String): String {
        return try {
            val response = chat.sendMessage(
                "Como experto asesor financiero, proporciona recomendaciones claras y prácticas sobre: $userMessage. " +
                        "Incluye consejos específicos, considera diferentes escenarios y mantén un tono profesional pero accesible."
            )
            response.text ?: "Lo siento, no pude generar recomendaciones en este momento."
        } catch (e: Exception) {
            "Error al comunicarse con el servicio de recomendaciones: ${e.localizedMessage}. Por favor, inténtalo de nuevo."
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