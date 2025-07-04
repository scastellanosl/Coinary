package com.example.coinary.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.coinary.ui.theme.BinaryQuestionCard
import com.example.coinary.ui.theme.ExplanationCard
import com.example.coinary.ui.theme.QuestionCard
import com.example.coinary.view.HomeScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UserSetupScreen(
    onSetupComplete: () -> Unit
) {
    val questions = listOf(
        "¿Cuánto dinero ganas al mes?",
        "¿Cuánto dinero usas en gastos fijos?",
        "¿Sabes cuánto gastas al mes en cosas no esenciales?",
        "¿Sabes qué son los gastos hormiga?",
        "¿De tus gastos no esenciales, cuánto crees que se va en gastos hormiga?"
    )

    var currentStep by remember { mutableStateOf(0) }
    val respuestas = remember { mutableMapOf<Int, String>() }

    var mostrarExplicacionGastosHormiga by remember { mutableStateOf(false) }
    var mostrarPreguntaGastosHormiga by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreen(navController = rememberNavController(), onLogout = {})

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn()) with
                            (slideOutHorizontally { fullWidth -> -fullWidth } + fadeOut())
                },
                label = "preguntas_animadas"
            ) { step ->
                when (step) {
                    in 0..2 -> {
                        QuestionCard(
                            question = questions[step],
                            onAnswer = { answer ->
                                respuestas[step] = answer
                                currentStep++
                            }
                        )
                    }

                    3 -> {
                        BinaryQuestionCard(
                            question = questions[step],
                            onAnswer = { saidYes ->
                                respuestas[step] = if (saidYes) "sí" else "no"
                                if (saidYes) {
                                    mostrarPreguntaGastosHormiga = true
                                    currentStep++ // pasamos a la siguiente
                                } else {
                                    mostrarExplicacionGastosHormiga = true
                                }
                            }
                        )
                    }

                    4 -> {
                        if (mostrarPreguntaGastosHormiga) {
                            QuestionCard(
                                question = questions[4],
                                onAnswer = { answer ->
                                    respuestas[4] = answer
                                    onSetupComplete()
                                }
                            )
                        } else {
                            ExplanationCard(
                                explanation = "Los gastos hormiga son pequeñas compras o consumos diarios que muchas veces no registramos. Por ejemplo: cafés, dulces, snacks, transporte extra, etc. Aunque parecen insignificantes, acumulados representan una porción importante del gasto mensual.",
                                onContinue = {
                                    mostrarExplicacionGastosHormiga = false
                                    mostrarPreguntaGastosHormiga = true
                                    currentStep++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

