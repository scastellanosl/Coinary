package com.example.coinary.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.model.Expense
import com.example.coinary.viewmodel.MovementViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// --- 1. PANTALLA PRINCIPAL (El esqueleto que faltaba) ---
@Composable
fun AntExpensesScreen(
    navController: NavController,
    viewModel: MovementViewModel = viewModel()
) {
    val systemUiController = rememberSystemUiController()
    val uiState by viewModel.uiState.collectAsState()

    // Obtenemos los gastos que el sistema marcó como hormiga (automática o manualmente)
    val antExpenses = uiState.antExpenses

    val totalAntMonth = antExpenses.sumOf { it.amount }
    val projectionYear = totalAntMonth * 12

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 }
    }
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }

    SideEffect {
        systemUiController.setStatusBarColor(color = Color.Black, darkIcons = false)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Fondo de la app
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 40.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // Header / Barra superior
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                Text(
                    text = "Gastos Hormiga",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont, // Asegúrate de tener InterFont disponible o bórralo si da error
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tu Tarjeta de Impacto
            ImpactCard(
                totalMonth = totalAntMonth,
                projectionYear = projectionYear,
                currencyFormat = currencyFormat
            )

            // Tu Insight Inteligente
            if (antExpenses.isNotEmpty()) {
                AntInsight(antExpenses = antExpenses)
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Detecciones automáticas de Coinary",
                color = Color.Gray,
                fontSize = 14.sp,
                fontFamily = InterFont,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Lista de Gastos
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                if (antExpenses.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "No se han detectado gastos hormiga aún.\nSigue registrando tus movimientos.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(antExpenses) { expense ->
                        AntExpenseItem(expense, currencyFormat, dateFormat)
                    }
                }
            }
        }
    }
}

// --- 2. TUS COMPONENTES (Integrados aquí) ---

@Composable
fun ImpactCard(totalMonth: Double, projectionYear: Double, currencyFormat: NumberFormat) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF150F33), Color(0xFF1B1A1A))
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(brush = gradientBrush)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(45.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Este mes has perdido",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = InterFont
            )

            Text(
                text = currencyFormat.format(totalMonth).replace("COP", "$"),
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Usamos Divider o HorizontalDivider según tu versión de Material3
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Proyección Anual: ",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = InterFont
                )
                Text(
                    text = currencyFormat.format(projectionYear).replace("COP", "$"),
                    color = Color(0xFFFF5252),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = InterFont
                )
            }
        }
    }
}

@Composable
fun AntInsight(antExpenses: List<Expense>) {
    val topCategory = antExpenses.groupBy { it.category }
        .maxByOrNull { it.value.size }?.key ?: "ninguna"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF150F33).copy(alpha = 0.5f)), // Ajustado ligeramente para contraste
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFD700)) // Amarillo dorado
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tu mayor fuga de dinero está en '$topCategory'. ¡Atento a esos pequeños consumos!",
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = InterFont,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun AntExpenseItem(expense: Expense, currencyFormat: NumberFormat, dateFormat: SimpleDateFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = expense.description, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = InterFont, maxLines = 1)
            Text(text = expense.category, color = Color.Gray, fontSize = 12.sp, fontFamily = InterFont)
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "-${currencyFormat.format(expense.amount).replace("COP", "$")}",
                color = Color(0xFFFF5252),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )
            Text(text = dateFormat.format(expense.date.toDate()), color = Color.Gray, fontSize = 11.sp, fontFamily = InterFont)
        }
    }
}