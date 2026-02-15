package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.viewmodel.RecommendationViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * RecomendacionesPantalla: Financial Recommendations Chat Interface.
 * Provides a conversational UI where users can ask for and receive
 * personalized financial advice based on their transaction history.
 *
 * @param navController Navigation controller for screen transitions.
 * @param recommendationViewModel ViewModel managing the recommendation logic and API calls.
 * @param onLogout Optional callback for logout actions.
 */
@Composable
fun RecomendacionesPantalla(
    navController: NavController,
    recommendationViewModel: RecommendationViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    // --- State Observation ---
    val uiState by recommendationViewModel.uiState.collectAsState()
    var message by remember { mutableStateOf("") }

    // --- System UI Configuration ---
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(Color.Black, darkIcons = false)
    }

    // --- Responsive Configuration ---
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val titleFontSize = if (screenWidth < 360.dp) 22.sp else 26.sp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- Header Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Financial Recommendations",
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Spacer to balance layout (centering the title)
                Spacer(modifier = Modifier.size(48.dp))
            }

            // --- Chat List Section ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.messages) { msg ->
                    // Using shared ChatBubble component (ensure this exists in the view package)
                    ChatBubble(message = msg)
                }

                if (uiState.isLoading) {
                    item {
                        // Using shared TypingIndicator component
                        TypingIndicator()
                    }
                }
            }

            // --- Input Field Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    placeholder = {
                        Text("Ask for financial advice...", color = Color.Gray)
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (message.isNotBlank()) {
                            recommendationViewModel.sendMessage(message)
                            message = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF4D54BF), shape = CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

/**
 * ChatMessage: Data model representing a single message in the chat.
 * Note: Consider moving this to a shared 'model' package if used across multiple screens.
 *
 * @param text The content of the message.
 * @param isUser True if the message is sent by the user, false if by the AI.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean
)