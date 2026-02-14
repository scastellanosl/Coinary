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
    initialTab: String,
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

    // Estados para selección (acciones existentes)
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    // Estados para CREACIÓN (Nuevos diálogos)
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

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

    // --- DIÁLOGOS DE ACCIÓN (Pagar / Aportar) ---
    selectedDebt?.let { debt ->
        DebtActionDialog(
            debt = debt,
            onDismiss = { selectedDebt = null },
            onPayment = { paymentAmount, isNewIncome ->
                viewModel.makePayment(debt.id, paymentAmount, isNewIncome)
                selectedDebt = null
            },
            onChangeDate = { newDate ->
                viewModel.updateDebtDate(debt.id, newDate)
                selectedDebt = null
            }
        )
    }

    selectedGoal?.let { goal ->
        GoalActionDialog(
            goal = goal,
            onDismiss = { selectedGoal = null },
            onContribute = { contributionAmount, isNewIncome ->
                viewModel.contributeToGoal(goal.id, contributionAmount, isNewIncome)
                selectedGoal = null
            },
            onChangeDate = { newDate ->
                viewModel.updateGoalDate(goal.id, newDate)
                selectedGoal = null
            }
        )
    }

    // --- DIÁLOGOS DE CREACIÓN ---
    if (showAddDebtDialog) {
        AddDebtDialog(
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { amount, description, creditor, date ->
                viewModel.addDebt(amount, description, creditor, date)
                showAddDebtDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, targetAmount, deadline ->
                viewModel.addGoal(name, targetAmount, deadline)
                showAddGoalDialog = false
            }
        )
    }

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
            // Header
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

            // Tabs
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

            // Contenido de la lista
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    "Deudas" -> {
                        if (uiState.debts.isEmpty() && !uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tienes deudas registradas", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontFamily = InterFont)
                            }
                        } else {
                            // padding bottom extra para que el botón no tape el último item
                            DebtsList(debts = uiState.debts, onDebtClick = { debt -> selectedDebt = debt })
                        }
                    }
                    "Metas" -> {
                        if (uiState.goals.isEmpty() && !uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No tienes metas registradas", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp, fontFamily = InterFont)
                            }
                        } else {
                            // padding bottom extra para que el botón no tape el último item
                            GoalsList(goals = uiState.goals, onGoalClick = { goal -> selectedGoal = goal })
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF4c6ef5))
                    }
                }
            }
        }

        // --- NUEVO BOTÓN INFERIOR ESTILO IMAGEN (MODIFICADO) ---
        Button(
            onClick = {
                if (selectedTab == "Deudas") {
                    showAddDebtDialog = true
                } else {
                    showAddGoalDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp) // MODIFICADO: Menos espacio desde el fondo (más abajo)
                .fillMaxWidth(0.65f) // MODIFICADO: Menos ancho (65% de la pantalla)
                .height(45.dp), // MODIFICADO: Menos altura
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD9D9D9) // Gris claro estilo imagen
            ),
            shape = RoundedCornerShape(22.5.dp) // Ajustado para que sea perfectamente redondo en los bordes
        ) {
            Text(
                text = if (selectedTab == "Deudas") "Añadir nueva deuda" else "Añadir nueva meta",
                color = Color.Black,
                fontSize = 14.sp, // MODIFICADO: Letra un poco más pequeña
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )
        }
    }
}

// ----------------------------------------------------------------------------------
// --- DIÁLOGOS DE ACCIÓN (PAGAR/APORTAR) CON CHECKBOX ---
// ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtActionDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onPayment: (Double, Boolean) -> Unit,
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
    var isNewIncome by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // DETECTAR SI ESTÁ COMPLETAMENTE PAGADA
    val isPaid = debt.isPaid || debt.amountPaid >= debt.amount

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = debt.description,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = InterFont,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                //  MOSTRAR BADGE SI ESTÁ PAGADA
                if (isPaid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = " Deuda Pagada Completamente",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currencyFormat.format(debt.amount).replace("COP", ""),
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

                // Barra de progreso
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
                                    colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))
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
                    text = "Restante: ${currencyFormat.format(remainingAmount).replace("COP", "")}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(24.dp))

                //  DESHABILITAR CAMPO SI ESTÁ PAGADA
                TextField(
                    value = paymentAmount,
                    onValueChange = { newValue ->
                        if (!isPaid) {
                            val cleaned = newValue.replace(".", "").replace(",", "")
                            if (cleaned.isEmpty()) {
                                paymentAmount = ""
                            } else if (cleaned.matches(Regex("\\d{0,12}"))) {
                                paymentAmount = cleaned
                            }
                        }
                    },
                    label = {
                        Text(
                            text = "Monto a pagar",
                            fontFamily = InterFont,
                            color = Color(0xFF868686)
                        )
                    },
                    enabled = !isPaid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = 16.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        disabledContainerColor = Color(0xFF2D2D2D),
                        disabledTextColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                //  DESHABILITAR CHECKBOX SI ESTÁ PAGADA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isPaid) { isNewIncome = !isNewIncome }
                ) {
                    Checkbox(
                        checked = isNewIncome,
                        onCheckedChange = { if (!isPaid) isNewIncome = it },
                        enabled = !isPaid,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4c6ef5),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White,
                            disabledCheckedColor = Color.Gray,
                            disabledUncheckedColor = Color.DarkGray
                        )
                    )
                    Text(
                        text = "Registrar como nuevo ingreso",
                        color = if (isPaid) Color.Gray else Color.White,
                        fontSize = 14.sp,
                        fontFamily = InterFont
                    )
                }

                Text(
                    text = "Marca si este dinero NO sale de tu saldo actual.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = InterFont,
                    modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                //  DESHABILITAR BOTÓN DE PAGAR SI ESTÁ PAGADA
                Button(
                    onClick = {
                        val amount = paymentAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amount > remainingAmount) {
                            Toast.makeText(
                                context,
                                "El monto excede la deuda restante",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        onPayment(amount, isNewIncome)
                    },
                    enabled = !isPaid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4c6ef5),
                        disabledContainerColor = Color.Gray //
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
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalActionDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onContribute: (Double, Boolean) -> Unit,
    onChangeDate: (Date) -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }}
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = goal.progressPercentage()
    val remainingAmount = goal.remainingAmount()

    var contributionAmount by remember { mutableStateOf("") }
    var isNewIncome by remember { mutableStateOf(false) }
    val context = LocalContext.current

    //  DETECTAR SI ESTÁ COMPLETADA
    val isCompleted = goal.isCompleted || goal.currentAmount >= goal.targetAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { },
        containerColor = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = goal.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = InterFont,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                //  MOSTRAR BADGE SI ESTÁ COMPLETADA
                if (isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = " Meta Completada",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = InterFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(
                            text = currencyFormat.format(goal.targetAmount).replace("COP", ""),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(text = "Meta", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormat.format(goal.deadline.toDate()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(text = "Fecha límite", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2D2D2D))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercentage / 100f)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Restante: ${currencyFormat.format(remainingAmount).replace("COP", "")}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(24.dp))

                //  DESHABILITAR CAMPO SI ESTÁ COMPLETADA
                TextField(
                    value = contributionAmount,
                    onValueChange = { newValue ->
                        if (!isCompleted) {
                            val cleaned = newValue.replace(".", "").replace(",", "")
                            if (cleaned.matches(Regex("\\d*"))) {
                                contributionAmount = cleaned
                            }
                        }
                    },
                    label = { Text("Monto a aportar", fontFamily = InterFont, color = Color(0xFF868686)) },
                    enabled = !isCompleted,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = 16.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledContainerColor = Color(0xFF2D2D2D),
                        disabledTextColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                //  DESHABILITAR CHECKBOX SI ESTÁ COMPLETADA
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCompleted) { isNewIncome = !isNewIncome }
                ) {
                    Checkbox(
                        checked = isNewIncome,
                        onCheckedChange = { if (!isCompleted) isNewIncome = it },
                        enabled = !isCompleted,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF4c6ef5),
                            uncheckedColor = Color.Gray,
                            checkmarkColor = Color.White
                        )
                    )
                    Text("Registrar como nuevo ingreso", color = Color.White, fontSize = 14.sp, fontFamily = InterFont)
                }
                Text(
                    text = "Marca si este dinero NO sale de tu saldo actual.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    //  DESHABILITAR BOTÓN DE APORTAR SI ESTÁ COMPLETADA
                    Button(
                        onClick = {
                            val amount = contributionAmount.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onContribute(amount, isNewIncome)
                        },
                        enabled = !isCompleted,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4c6ef5),
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aportar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFont)
                    }

                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.time = goal.deadline.toDate()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newDate = Calendar.getInstance().apply { set(year, month, day) }.time
                                    onChangeDate(newDate)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cambiar Fecha", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFont)
                    }
                }
            }
        }
    )
}


// ----------------------------------------------------------------------------------
// --- COMPONENTES AUXILIARES ---
// ----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String, Date) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creditor by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            selectedDate = calendar.time
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Nueva Deuda", color = Color.White, fontFamily = InterFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = amount,
                    onValueChange = {
                        val cleaned = it.replace(".", "").replace(",", "")
                        if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = cleaned
                    },
                    label = { Text("Monto total", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción (ej. Tarjeta)", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                TextField(
                    value = creditor,
                    onValueChange = { creditor = it },
                    label = { Text("Acreedor (ej. Banco)", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fecha Límite: ${dateFormat.format(selectedDate)}", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (amountDouble != null && description.isNotEmpty() && creditor.isNotEmpty()) {
                        onConfirm(amountDouble, description, creditor, selectedDate)
                    } else {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c6ef5))
            ) {
                Text("Crear", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Date) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }

    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            selectedDate = calendar.time
        },
        Calendar.getInstance().get(Calendar.YEAR),
        Calendar.getInstance().get(Calendar.MONTH),
        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        title = { Text("Nueva Meta", color = Color.White, fontFamily = InterFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la meta", color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                TextField(
                    value = targetAmount,
                    onValueChange = {
                        val cleaned = it.replace(".", "").replace(",", "")
                        if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) targetAmount = cleaned
                    },
                    label = { Text("Monto Objetivo", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fecha Objetivo: ${dateFormat.format(selectedDate)}", color = Color.White)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = targetAmount.toDoubleOrNull()
                    if (amountDouble != null && name.isNotEmpty()) {
                        onConfirm(name, amountDouble, selectedDate)
                    } else {
                        Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c6ef5))
            ) {
                Text("Crear", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
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
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFont)
    }
}

@Composable
fun DebtsList(debts: List<Debt>, onDebtClick: (Debt) -> Unit) {
    val totalDebt = debts.sumOf { it.amount }
    val totalPaid = debts.sumOf { it.amountPaid }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // AUMENTAMOS PADDING PARA QUE EL BOTÓN NO TAPE EL ULTIMO ITEM
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(debts) { debt -> DebtCard(debt = debt, onClick = { onDebtClick(debt) }) }
        if (debts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                TotalDebtSummaryCard(totalDebt = totalDebt, totalPaid = totalPaid)
            }
        }
    }
}

@Composable
fun GoalsList(goals: List<SavingsGoal>, onGoalClick: (SavingsGoal) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        // AUMENTAMOS PADDING PARA QUE EL BOTÓN NO TAPE EL ULTIMO ITEM
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(goals) { goal -> GoalCard(goal = goal, onClick = { onGoalClick(goal) }) }
    }
}

@Composable
fun DebtCard(debt: Debt, onClick: () -> Unit) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = debt.progressPercentage()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF2A2A3E), Color(0xFF1E1E2E))))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(text = debt.description, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateFormat.format(debt.dueDate.toDate()), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = currencyFormat.format(debt.amount).replace("COP", "$"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont)
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2D2D2D))) {
                        Box(modifier = Modifier.fillMaxWidth(progressPercentage / 100f).fillMaxHeight().background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))), shape = RoundedCornerShape(4.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "llevas un progreso del ${progressPercentage.toInt()}%", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = InterFont)
                }
            }
        }
    }
}

@Composable
fun GoalCard(goal: SavingsGoal, onClick: () -> Unit) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val progressPercentage = goal.progressPercentage()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF2A2A3E), Color(0xFF1E1E2E))))) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(text = goal.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dateFormat.format(goal.deadline.toDate()), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = currencyFormat.format(goal.targetAmount).replace("COP", "$"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont)
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2D2D2D))) {
                        Box(modifier = Modifier.fillMaxWidth(progressPercentage / 100f).fillMaxHeight().background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))), shape = RoundedCornerShape(4.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "llevas un progreso del ${progressPercentage.toInt()}%", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = InterFont)
                }
            }
        }
    }
}

@Composable
fun TotalDebtSummaryCard(totalDebt: Double, totalPaid: Double) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val totalRemaining = totalDebt - totalPaid
    val progressPercentage = if (totalDebt > 0) ((totalPaid / totalDebt) * 100).toFloat() else 0f

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Box(modifier = Modifier.fillMaxWidth().background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF3D3D5C), Color(0xFF2A2A45))))) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Total de Deudas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f), fontFamily = InterFont)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = currencyFormat.format(totalRemaining).replace("COP", "$"), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont)
                    }
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Total deudas", tint = Color(0xFF4c6ef5), modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = "Deuda total", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                        Text(text = currencyFormat.format(totalDebt).replace("COP", "$"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White, fontFamily = InterFont)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Pagado", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                        Text(text = currencyFormat.format(totalPaid).replace("COP", "$"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4c6ef5), fontFamily = InterFont)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2D2D2D))) {
                    Box(modifier = Modifier.fillMaxWidth(progressPercentage / 100f).fillMaxHeight().background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))), shape = RoundedCornerShape(4.dp)))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Progreso general: ${progressPercentage.toInt()}%", fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = InterFont)
            }
        }
    }
}