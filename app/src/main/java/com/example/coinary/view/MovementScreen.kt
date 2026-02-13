package com.example.coinary.view

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.utils.ThousandSeparatorTransformation
import com.example.coinary.viewmodel.MovementViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementScreen(
    navController: NavController,
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
){
    val movementViewModel: MovementViewModel = viewModel()
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val uiState by movementViewModel.uiState.collectAsState()

    // Estados del formulario
    var selectedMovementType by remember { mutableStateOf(context.getString(R.string.income)) }
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(context.getString(R.string.select_category)) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Configuración de UI
    SideEffect {
        systemUiController.setStatusBarColor(color = Color.Black, darkIcons = false)
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val titleFontSize = if (screenWidth < 360.dp) 22.sp else 26.sp
    val labelFontSize = if (screenWidth < 360.dp) 14.sp else 16.sp
    val buttonHeight = if (screenHeight < 600.dp) 40.dp else 44.dp
    val buttonHorizontalPadding = if (screenWidth < 360.dp) 12.dp else 20.dp

    val incomeCategories = remember { context.resources.getStringArray(R.array.income_categories).toList() }
    val expenseCategories = remember { context.resources.getStringArray(R.array.expense_categories).toList() }
    val currentCategories = if (selectedMovementType == context.getString(R.string.income)) incomeCategories else expenseCategories

    val commonFieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = buttonHorizontalPadding)
        .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(12.dp))

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Efectos de guardado exitoso/error
    LaunchedEffect(uiState) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            amount = ""; description = ""; selectedCategory = context.getString(R.string.select_category)
            selectedDate = Calendar.getInstance()
            movementViewModel.resetMessages()
            navController.popBackStack()
        }
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            movementViewModel.resetMessages()
        }
    }

    // Alerta de Deuda Automática
    if (uiState.showDebtConfirmation) {
        AlertDialog(
            onDismissRequest = { movementViewModel.cancelDebtCreation() },
            containerColor = Color(0xFF1E1E1E),
            title = { Text(text = "¡Saldo Insuficiente!", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontFamily = InterFont) },
            text = { Text(text = "El gasto supera tus ingresos. ¿Crear deuda?", color = Color.White, fontFamily = InterFont) },
            confirmButton = { Button(onClick = { movementViewModel.confirmDebtCreation() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))) { Text("Sí", color = Color.White) } },
            dismissButton = { TextButton(onClick = { movementViewModel.cancelDebtCreation() }) { Text("Cancelar", color = Color.White) } }
        )
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.fondo_movimentos),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 40.dp)
        )

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(scrollState)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(top = 1.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = stringResource(R.string.notifications), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(text = stringResource(R.string.add_movement), fontFamily = InterFont, fontSize = titleFontSize, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(30.dp))

            // Botones Tipo
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                MovementTypeButton(text = stringResource(R.string.income), isSelected = selectedMovementType == stringResource(R.string.income), onClick = { selectedMovementType = context.getString(R.string.income); selectedCategory = context.getString(R.string.select_category) }, modifier = Modifier.weight(1f).height(buttonHeight), backgroundColor = Color(0xFF4c6ef5), textColor = Color.White)
                MovementTypeButton(text = stringResource(R.string.expense), isSelected = selectedMovementType == context.getString(R.string.expense), onClick = { selectedMovementType = context.getString(R.string.expense); selectedCategory = context.getString(R.string.select_category) }, modifier = Modifier.weight(1f).height(buttonHeight), backgroundColor = Color.White, textColor = Color.Black)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Contenedor Campos
            Box(modifier = Modifier.align(Alignment.CenterHorizontally).padding(15.dp)) {
                Image(painter = painterResource(id = R.drawable.fondo_contenedor_categoria), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)) {
                    Text(text = stringResource(R.string.select_category), fontFamily = InterFont, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = (labelFontSize.value + 4).sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp))

                    // Categoría
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = commonFieldModifier) {
                        TextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.select_category), color = Color(0xFF868686)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = labelFontSize), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, unfocusedTextColor = Color.White, focusedTextColor = Color.White))
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.9f))) {
                            currentCategories.forEach { category ->
                                DropdownMenuItem(text = { Text(fontFamily = InterFont, fontWeight = FontWeight.Normal, fontSize = labelFontSize, text = category, color = Color.White) }, onClick = { selectedCategory = category; expanded = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Monto
                    TextField(
                        value = amount,
                        onValueChange = { newValue ->
                            val cleaned = newValue.replace(".", "").replace(",", "")
                            if (cleaned.isEmpty()) amount = "" else if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = cleaned
                        },
                        label = { Text(text = stringResource(R.string.amount), fontFamily = InterFont, fontWeight = FontWeight.Normal, fontSize = labelFontSize, color = Color(0xFF868686)) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandSeparatorTransformation(), modifier = commonFieldModifier,
                        textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = labelFontSize),
                        leadingIcon = { Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = Color.White) },
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, unfocusedTextColor = Color.White, focusedTextColor = Color.White)
                    )

                    // [TEXTO DE AVISO HORMIGA ELIMINADO AQUÍ]

                    Spacer(modifier = Modifier.height(16.dp))

                    // Fecha
                    val datePickerDialog = DatePickerDialog(context, { _: DatePicker, y: Int, m: Int, d: Int -> selectedDate.set(y, m, d) }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

                    TextField(value = dateFormatter.format(selectedDate.time), onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.date), color = Color(0xFF868686)) }, singleLine = true, modifier = commonFieldModifier.clickable { datePickerDialog.show() }, textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = labelFontSize), leadingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White) }, trailingIcon = { Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, unfocusedTextColor = Color.White, focusedTextColor = Color.White))

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(text = stringResource(R.string.short_description), color = Color.White, fontFamily = InterFont, fontSize = (labelFontSize.value - 2).sp, fontWeight = FontWeight.Thin, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 30.dp))
                    Spacer(modifier = Modifier.height(15.dp))

                    // Descripción
                    TextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.add_description), color = Color(0xFF868686)) }, singleLine = true, modifier = commonFieldModifier, textStyle = TextStyle(fontFamily = InterFont, color = Color.White, fontSize = labelFontSize), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, unfocusedTextColor = Color.White, focusedTextColor = Color.White))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botones
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        MovementTypeButton(
                            text = stringResource(R.string.save),
                            isSelected = false,
                            onClick = {
                                val amt = amount.toDoubleOrNull()
                                if (amt == null || description.isBlank() || selectedCategory == context.getString(R.string.select_category)) {
                                    Toast.makeText(context, context.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                                    return@MovementTypeButton
                                }
                                if (selectedMovementType == context.getString(R.string.income)) {
                                    movementViewModel.saveIncome(amt, description, selectedCategory, selectedDate.time)
                                } else {
                                    // La lógica "Pro" sigue funcionando internamente
                                    movementViewModel.saveExpensePro(amt, description, selectedCategory, selectedDate.time)
                                }
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !uiState.isLoading,
                            backgroundColor = Color(0xFF4c6ef5),
                            textColor = Color.White,
                            isLoading = uiState.isLoading
                        )
                        MovementTypeButton(text = stringResource(R.string.cancel), isSelected = false, onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f).height(50.dp), backgroundColor = Color.White, textColor = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun MovementTypeButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Color(0xFF4c6ef5),
    textColor: Color = Color.White,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) backgroundColor else Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(backgroundColor, backgroundColor))
        ) else null,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        enabled = enabled
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text = text, color = if (isSelected) textColor else backgroundColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFont)
        }
    }
}