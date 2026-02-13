package com.example.coinary.view

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.coinary.R
import com.example.coinary.model.Debt
import com.example.coinary.model.SavingsGoal
import com.example.coinary.utils.ThousandSeparatorTransformation
import com.example.coinary.viewmodel.DebtGoalViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DebtsAndGoalsScreen(
    navController: NavController,
    initialTab: String, // <--- 1. AGREGAMOS ESTE PARÁMETRO
    viewModel: DebtGoalViewModel = viewModel(),
    onBackClick: () -> Unit = { navController.popBackStack() }
) {
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Black,
            darkIcons = false
        )
    }

    val startTab = if (initialTab.equals("ahorros", ignoreCase = true)) "Metas" else "Deudas"

    var selectedTab by remember { mutableStateOf(startTab) }
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    // Mostrar mensajes
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.resetMessages()
        }
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.resetMessages()
        }
    }

    // Mostrar diálogo de deuda
    selectedDebt?.let { debt ->
        DebtActionDialog(
            debt = debt,
            onDismiss = { selectedDebt = null },
            onPayment = { paymentAmount ->
                viewModel.makePayment(debt.id, paymentAmount)
                selectedDebt = null
            },
            onChangeDate = { newDate ->
                viewModel.updateDebtDate(debt.id, newDate)
                selectedDebt = null
            }
        )
    }

    // Mostrar diálogo de meta
    selectedGoal?.let { goal ->
        GoalActionDialog(
            goal = goal,
            onDismiss = { selectedGoal = null },
            onContribute = { contributionAmount ->
                viewModel.contributeToGoal(goal.id, contributionAmount)
                selectedGoal = null
            },
            onChangeDate = { newDate ->
                viewModel.updateGoalDate(goal.id, newDate)
                selectedGoal = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                TabButton(
                    text = "Deudas",
                    isSelected = selectedTab == "Deudas",
                    onClick = { selectedTab = "Deudas" },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = "Metas",
                    isSelected = selectedTab == "Metas",
                    onClick = { selectedTab = "Metas" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    "Deudas" -> {
                        if (uiState.debts.isEmpty() && !uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tienes deudas registradas",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontFamily = InterFont
                                )
                            }
                        } else {
                            DebtsList(
                                debts = uiState.debts,
                                onDebtClick = { debt -> selectedDebt = debt }
                            )
                        }
                    }
                    "Metas" -> {
                        if (uiState.goals.isEmpty() && !uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tienes metas registradas",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontFamily = InterFont
                                )
                            }
                        } else {
                            GoalsList(
                                goals = uiState.goals,
                                onGoalClick = { goal -> selectedGoal = goal }
                            )
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4c6ef5))
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4c6ef5) else Color.Transparent,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(20.dp),
        border = if (!isSelected) {
            ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(listOf(Color.White, Color.White))
            )
        } else null,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFont
        )
    }
}

@Composable
fun DebtsList(
    debts: List<Debt>,
    onDebtClick: (Debt) -> Unit
) {
    val totalDebt = debts.sumOf { it.amount }
    val totalPaid = debts.sumOf { it.amountPaid }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(debts) { debt ->
            DebtCard(debt = debt, onClick = { onDebtClick(debt) })
        }

        // Tarjeta de resumen al final
        if (debts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TotalDebtSummaryCard(
                    totalDebt = totalDebt,
                    totalPaid = totalPaid
                )
            }
        }
    }
}


@Composable
fun GoalsList(
    goals: List<SavingsGoal>,
    onGoalClick: (SavingsGoal) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(goals) { goal ->
            GoalCard(goal = goal, onClick = { onGoalClick(goal) })
        }
    }
}

@Composable
fun DebtCard(
    debt: Debt,
    onClick: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = debt.progressPercentage()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A2A3E),
                            Color(0xFF1E1E2E)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = debt.description,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = InterFont,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = dateFormat.format(debt.dueDate.toDate()),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = InterFont
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currencyFormat.format(debt.amount).replace("COP", "$"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2D2D2D))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercentage / 100f)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4c6ef5),
                                            Color(0xFF5E7EFF)
                                        )
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "llevas un progreso del ${progressPercentage.toInt()}%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = InterFont
                    )
                }
            }
        }
    }
}

@Composable
fun GoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = goal.progressPercentage()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2A2A3E),
                            Color(0xFF1E1E2E)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = goal.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = InterFont,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = dateFormat.format(goal.deadline.toDate()),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = InterFont
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currencyFormat.format(goal.targetAmount).replace("COP", "$"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2D2D2D))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressPercentage / 100f)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4c6ef5),
                                            Color(0xFF5E7EFF)
                                        )
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "llevas un progreso del ${progressPercentage.toInt()}%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = InterFont
                    )
                }
            }
        }
    }
}

@Composable
fun TotalDebtSummaryCard(
    totalDebt: Double,
    totalPaid: Double
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }

    val totalRemaining = totalDebt - totalPaid
    val progressPercentage = if (totalDebt > 0) ((totalPaid / totalDebt) * 100).toFloat() else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3D3D5C),
                            Color(0xFF2A2A45)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total de Deudas",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = InterFont
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = currencyFormat.format(totalRemaining).replace("COP", "$"),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Total deudas",
                        tint = Color(0xFF4c6ef5),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Información adicional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Deuda total",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = InterFont
                        )
                        Text(
                            text = currencyFormat.format(totalDebt).replace("COP", "$"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Pagado",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = InterFont
                        )
                        Text(
                            text = currencyFormat.format(totalPaid).replace("COP", "$"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4c6ef5),
                            fontFamily = InterFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Barra de progreso total
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2D2D2D))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercentage / 100f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4c6ef5),
                                        Color(0xFF5E7EFF)
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Progreso general: ${progressPercentage.toInt()}%",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtActionDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onPayment: (Double) -> Unit,
    onChangeDate: (Date) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = debt.progressPercentage()
    val remainingAmount = debt.remainingBalance()

    var paymentAmount by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { /* Vacío, los botones están en el content */ },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = debt.description,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = InterFont,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currencyFormat.format(debt.amount).replace("COP", "$"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(
                            text = "Deuda total",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = InterFont
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormat.format(debt.dueDate.toDate()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(
                            text = "Fecha límite",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = InterFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2D2D2D))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercentage / 100f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4c6ef5),
                                        Color(0xFF5E7EFF)
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "llevas un progreso del ${progressPercentage.toInt()}%",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Restante: ${currencyFormat.format(remainingAmount).replace("COP", "$")}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = paymentAmount,
                    onValueChange = { newValue ->
                        val cleaned = newValue.replace(".", "").replace(",", "")
                        if (cleaned.isEmpty()) {
                            paymentAmount = ""
                        } else if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            paymentAmount = cleaned
                        }
                    },
                    label = {
                        Text(
                            text = "Monto a pagar",
                            fontFamily = InterFont,
                            color = Color(0xFF868686)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    textStyle = TextStyle(
                        fontFamily = InterFont,
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val amount = paymentAmount.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (amount > remainingAmount) {
                                Toast.makeText(context, "El monto excede la deuda restante", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onPayment(amount)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4c6ef5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Pagar",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFont
                        )
                    }

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = debt.dueDate.toDate()

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newDate = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }.time
                                    onChangeDate(newDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cambiar Fecha",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFont
                        )
                    }
                }
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalActionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit,
    onChangeDate: (Date) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = goal.progressPercentage()
    val remainingAmount = goal.remainingAmount()

    var contributionAmount by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { /* Vacío, los botones están en el content */ },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = goal.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = InterFont,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currencyFormat.format(goal.targetAmount).replace("COP", "$"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(
                            text = "Meta",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = InterFont
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormat.format(goal.deadline.toDate()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(
                            text = "Fecha límite",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontFamily = InterFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF2D2D2D))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercentage / 100f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4c6ef5),
                                        Color(0xFF5E7EFF)
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "llevas un progreso del ${progressPercentage.toInt()}%",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Restante: ${currencyFormat.format(remainingAmount).replace("COP", "$")}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = contributionAmount,
                    onValueChange = { newValue ->
                        val cleaned = newValue.replace(".", "").replace(",", "")
                        if (cleaned.isEmpty()) {
                            contributionAmount = ""
                        } else if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            contributionAmount = cleaned
                        }
                    },
                    label = {
                        Text(
                            text = "Monto a aportar",
                            fontFamily = InterFont,
                            color = Color(0xFF868686)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    textStyle = TextStyle(
                        fontFamily = InterFont,
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val amount = contributionAmount.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onContribute(amount)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4c6ef5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Aportar",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFont
                        )
                    }

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = goal.deadline.toDate()

                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newDate = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }.time
                                    onChangeDate(newDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cambiar Fecha",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFont
                        )
                    }
                }
            }
        }
    )
}


@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewDebtCard() {
    DebtCard(
        debt = Debt(
            id = "1",
            amount = 2000000.0,
            amountPaid = 1400000.0,
            description = "deuda por compras",
            creditor = "Banco",
            dueDate = Timestamp(Date()),
            isPaid = false
        ),
        onClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewGoalCard() {
    GoalCard(
        goal = SavingsGoal(
            id = "1",
            name = "Celular nuevo",
            targetAmount = 1800000.0,
            currentAmount = 900000.0,
            deadline = Timestamp(Date()),
            isCompleted = false
        ),
        onClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewTotalDebtSummaryCard() {
    TotalDebtSummaryCard(
        totalDebt = 2078000.0,
        totalPaid = 1405800.0
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewDebtsList() {
    val mockDebts = listOf(
        Debt(
            id = "1",
            amount = 2000000.0,
            amountPaid = 1400000.0,
            description = "deuda por compras",
            creditor = "Banco",
            dueDate = Timestamp(Date(126, 2, 17)),
            isPaid = false
        ),
        Debt(
            id = "2",
            amount = 58000.0,
            amountPaid = 5800.0,
            description = "dinero obtenido en préstamo",
            creditor = "Amigo",
            dueDate = Timestamp(Date(126, 1, 2)),
            isPaid = false
        ),
        Debt(
            id = "3",
            amount = 20000.0,
            amountPaid = 0.0,
            description = "Materiales de la universidad",
            creditor = "Universidad",
            dueDate = Timestamp(Date(126, 1, 15)),
            isPaid = false
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        DebtsList(
            debts = mockDebts,
            onDebtClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewGoalsList() {
    val mockGoals = listOf(
        SavingsGoal(
            id = "1",
            name = "Celular nuevo",
            targetAmount = 1800000.0,
            currentAmount = 900000.0,
            deadline = Timestamp(Date(126, 5, 1)),
            isCompleted = false
        ),
        SavingsGoal(
            id = "2",
            name = "TV 55 Samsung",
            targetAmount = 2200000.0,
            currentAmount = 880000.0,
            deadline = Timestamp(Date(126, 11, 24)),
            isCompleted = false
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        GoalsList(
            goals = mockGoals,
            onGoalClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewDebtActionDialog() {
    DebtActionDialog(
        debt = Debt(
            id = "1",
            amount = 2000000.0,
            amountPaid = 1400000.0,
            description = "deuda por compras",
            creditor = "Banco",
            dueDate = Timestamp(Date()),
            isPaid = false
        ),
        onDismiss = {},
        onPayment = {},
        onChangeDate = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewGoalActionDialog() {
    GoalActionDialog(
        goal = SavingsGoal(
            id = "1",
            name = "Celular nuevo",
            targetAmount = 1800000.0,
            currentAmount = 900000.0,
            deadline = Timestamp(Date()),
            isCompleted = false
        ),
        onDismiss = {},
        onContribute = {},
        onChangeDate = {}
    )
}


@Preview(showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true)
@Composable
fun PreviewDebtsAndGoalsScreenComplete() {
    // Datos mock
    val mockDebts = listOf(
        Debt(
            id = "1",
            amount = 2000000.0,
            amountPaid = 1400000.0,
            description = "deuda por compras",
            creditor = "Banco",
            dueDate = Timestamp(Date(126, 2, 17)),
            isPaid = false
        ),
        Debt(
            id = "2",
            amount = 58000.0,
            amountPaid = 5800.0,
            description = "dinero obtenido en préstamo",
            creditor = "Amigo",
            dueDate = Timestamp(Date(126, 1, 2)),
            isPaid = false
        ),
        Debt(
            id = "3",
            amount = 20000.0,
            amountPaid = 0.0,
            description = "Materiales de la universidad",
            creditor = "Universidad",
            dueDate = Timestamp(Date(126, 1, 15)),
            isPaid = false
        )
    )

    val mockGoals = listOf(
        SavingsGoal(
            id = "1",
            name = "Celular nuevo",
            targetAmount = 1800000.0,
            currentAmount = 900000.0,
            deadline = Timestamp(Date(126, 5, 1)),
            isCompleted = false
        ),
        SavingsGoal(
            id = "2",
            name = "TV 55 Samsung",
            targetAmount = 2200000.0,
            currentAmount = 880000.0,
            deadline = Timestamp(Date(126, 11, 24)),
            isCompleted = false
        )
    )

    var selectedTab by remember { mutableStateOf("Deudas") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fondo
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Barra superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de pestañas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                TabButton(
                    text = "Deudas",
                    isSelected = selectedTab == "Deudas",
                    onClick = { selectedTab = "Deudas" },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = "Metas",
                    isSelected = selectedTab == "Metas",
                    onClick = { selectedTab = "Metas" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contenido
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    "Deudas" -> {
                        DebtsList(
                            debts = mockDebts,
                            onDebtClick = {}
                        )
                    }
                    "Metas" -> {
                        GoalsList(
                            goals = mockGoals,
                            onGoalClick = {}
                        )
                    }
                }
            }

            // Navigation Bar (simulado)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Home
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Botón Stats
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Stats",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Botón Add
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Botón Edit
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// Preview con la pestaña de Metas seleccionada
@Preview(showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true)
@Composable
fun PreviewDebtsAndGoalsScreenMetas() {
    val mockGoals = listOf(
        SavingsGoal(
            id = "1",
            name = "Celular nuevo",
            targetAmount = 1800000.0,
            currentAmount = 900000.0,
            deadline = Timestamp(Date(126, 5, 1)),
            isCompleted = false
        ),
        SavingsGoal(
            id = "2",
            name = "TV 55 Samsung",
            targetAmount = 2200000.0,
            currentAmount = 880000.0,
            deadline = Timestamp(Date(126, 11, 24)),
            isCompleted = false
        )
    )

    var selectedTab by remember { mutableStateOf("Metas") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                TabButton(
                    text = "Deudas",
                    isSelected = selectedTab == "Deudas",
                    onClick = { selectedTab = "Deudas" },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = "Metas",
                    isSelected = selectedTab == "Metas",
                    onClick = { selectedTab = "Metas" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                GoalsList(
                    goals = mockGoals,
                    onGoalClick = {}
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Stats",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}


