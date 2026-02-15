package com.example.coinary.view

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    // Logic for initial tab (mapped to English internal keys)
    val startTab = if (initialTab.equals("ahorros", ignoreCase = true)) "Goals" else "Debts"

    var selectedTab by remember { mutableStateOf(startTab) }

    // Estados para selección
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    var selectedGoal by remember { mutableStateOf<SavingsGoal?>(null) }

    // Estados para CREACIÓN
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

    // --- DIÁLOGOS DE ACCIÓN ---
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
                        contentDescription = stringResource(R.string.back_desc),
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications_desc),
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
                    text = stringResource(R.string.tab_debts),
                    isSelected = selectedTab == "Debts",
                    onClick = { selectedTab = "Debts" },
                    modifier = Modifier.weight(1f)
                )

                TabButton(
                    text = stringResource(R.string.tab_goals),
                    isSelected = selectedTab == "Goals",
                    onClick = { selectedTab = "Goals" },
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
                    "Debts" -> {
                        if (uiState.debts.isEmpty() && !uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.no_debts_registered),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontFamily = InterFont
                                )
                            }
                        } else {
                            DebtsList(debts = uiState.debts, onDebtClick = { debt -> selectedDebt = debt })
                        }
                    }
                    "Goals" -> {
                        if (uiState.goals.isEmpty() && !uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    stringResource(R.string.no_goals_registered),
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontFamily = InterFont
                                )
                            }
                        } else {
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

        // --- BOTÓN INFERIOR ---
        Button(
            onClick = {
                if (selectedTab == "Debts") {
                    showAddDebtDialog = true
                } else {
                    showAddGoalDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .fillMaxWidth(0.65f)
                .height(45.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD9D9D9)
            ),
            shape = RoundedCornerShape(22.5.dp)
        ) {
            Text(
                text = if (selectedTab == "Debts") stringResource(R.string.btn_add_new_debt) else stringResource(R.string.btn_add_new_goal),
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFont
            )
        }
    }
}

// ----------------------------------------------------------------------------------
// --- DIÁLOGOS DE ACCIÓN ---
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
                            text = stringResource(R.string.badge_debt_paid),
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
                            text = stringResource(R.string.label_debt_total),
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
                            text = stringResource(R.string.label_deadline),
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
                    text = stringResource(R.string.label_progress_text, progressPercentage.toInt()),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = InterFont,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))
                val remainingStr = currencyFormat.format(remainingAmount).replace("COP", "")
                Text(
                    text = stringResource(R.string.label_remaining, remainingStr),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            text = stringResource(R.string.label_pay_amount),
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
                        text = stringResource(R.string.checkbox_new_income),
                        color = if (isPaid) Color.Gray else Color.White,
                        fontSize = 14.sp,
                        fontFamily = InterFont
                    )
                }

                Text(
                    text = stringResource(R.string.msg_money_source),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = InterFont,
                    modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val amount = paymentAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(context, context.getString(R.string.msg_invalid_amount), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (amount > remainingAmount) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_amount_exceeds),
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
                        disabledContainerColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_pay),
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
                            text = stringResource(R.string.badge_goal_completed),
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
                        Text(text = stringResource(R.string.label_goal_target), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormat.format(goal.deadline.toDate()),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = InterFont
                        )
                        Text(text = stringResource(R.string.label_deadline), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
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
                val remainingStr = currencyFormat.format(remainingAmount).replace("COP", "")
                Text(
                    text = stringResource(R.string.label_remaining, remainingStr),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontFamily = InterFont
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    label = { Text(stringResource(R.string.label_contribute_amount), fontFamily = InterFont, color = Color(0xFF868686)) },
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
                    Text(stringResource(R.string.checkbox_new_income), color = Color.White, fontSize = 14.sp, fontFamily = InterFont)
                }
                Text(
                    text = stringResource(R.string.msg_money_source),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val amount = contributionAmount.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                Toast.makeText(context, context.getString(R.string.msg_invalid_amount), Toast.LENGTH_SHORT).show()
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
                        Text(stringResource(R.string.btn_contribute), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFont)
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
                        Text(stringResource(R.string.btn_change_date), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFont)
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
        title = { Text(stringResource(R.string.title_new_debt), color = Color.White, fontFamily = InterFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = amount,
                    onValueChange = {
                        val cleaned = it.replace(".", "").replace(",", "")
                        if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = cleaned
                    },
                    label = { Text(stringResource(R.string.label_amount_total), color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = ThousandSeparatorTransformation(),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.label_description_hint), color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                TextField(
                    value = creditor,
                    onValueChange = { creditor = it },
                    label = { Text(stringResource(R.string.label_creditor_hint), color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dateStr = dateFormat.format(selectedDate)
                    Text(stringResource(R.string.btn_date_deadline, dateStr), color = Color.White)
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
                        Toast.makeText(context, context.getString(R.string.msg_fill_all_fields), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c6ef5))
            ) {
                Text(stringResource(R.string.btn_create), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Color.White)
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
        title = { Text(stringResource(R.string.title_new_goal), color = Color.White, fontFamily = InterFont) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name_goal_hint), color = Color.Gray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )
                TextField(
                    value = targetAmount,
                    onValueChange = {
                        val cleaned = it.replace(".", "").replace(",", "")
                        if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) targetAmount = cleaned
                    },
                    label = { Text(stringResource(R.string.label_amount_target), color = Color.Gray) },
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
                    val dateStr = dateFormat.format(selectedDate)
                    Text(stringResource(R.string.btn_date_target, dateStr), color = Color.White)
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
                        Toast.makeText(context, context.getString(R.string.msg_fill_all_fields), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4c6ef5))
            ) {
                Text(stringResource(R.string.btn_create), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = Color.White)
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
                    Text(text = stringResource(R.string.label_progress_text, progressPercentage.toInt()), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = InterFont)
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
                    Text(text = stringResource(R.string.label_progress_text, progressPercentage.toInt()), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = InterFont)
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
                        Text(text = stringResource(R.string.title_total_debts_summary), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f), fontFamily = InterFont)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = currencyFormat.format(totalRemaining).replace("COP", "$"), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = InterFont)
                    }
                    Icon(imageVector = Icons.Default.AccountBalance, contentDescription = "Total deudas", tint = Color(0xFF4c6ef5), modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(text = stringResource(R.string.label_debt_total), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                        Text(text = currencyFormat.format(totalDebt).replace("COP", "$"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White, fontFamily = InterFont)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = stringResource(R.string.label_paid), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), fontFamily = InterFont)
                        Text(text = currencyFormat.format(totalPaid).replace("COP", "$"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4c6ef5), fontFamily = InterFont)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF2D2D2D))) {
                    Box(modifier = Modifier.fillMaxWidth(progressPercentage / 100f).fillMaxHeight().background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF4c6ef5), Color(0xFF5E7EFF))), shape = RoundedCornerShape(4.dp)))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = stringResource(R.string.label_progress_general, progressPercentage.toInt()), fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), fontFamily = InterFont)
            }
        }
    }
}