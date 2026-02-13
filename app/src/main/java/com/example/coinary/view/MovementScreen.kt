package com.example.coinary.view

import android.app.DatePickerDialog
import android.widget.DatePicker
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
fun AddMovementScreen(
    navController: NavController,
    // CORRECCIÓN: Quitamos el ViewModel de los parámetros para evitar VerifyError
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // CORRECCIÓN: Inicializamos el ViewModel aquí dentro
    val movementViewModel: MovementViewModel = viewModel()

    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val titleFontSize = if (screenWidth < 360.dp) 22.sp else 26.sp
    val labelFontSize = if (screenWidth < 360.dp) 14.sp else 16.sp
    val buttonHeight = if (screenHeight < 600.dp) 40.dp else 44.dp
    val buttonHorizontalPadding = if (screenWidth < 360.dp) 12.dp else 20.dp

    val context = LocalContext.current

    var selectedMovementType by remember { mutableStateOf(context.getString(R.string.income)) }

    var expanded by remember { mutableStateOf(false) }

    val incomeCategories = remember { context.resources.getStringArray(R.array.income_categories).toList() }
    val expenseCategories = remember { context.resources.getStringArray(R.array.expense_categories).toList() }
    val currentCategories = if (selectedMovementType == context.getString(R.string.income)) incomeCategories else expenseCategories

    var selectedCategory by remember { mutableStateOf(context.getString(R.string.select_category)) }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val commonFieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = buttonHorizontalPadding)
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        )

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val uiState by movementViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            amount = ""
            description = ""
            selectedCategory = context.getString(R.string.select_category)
            selectedDate = Calendar.getInstance()
            movementViewModel.resetMessages()
            navController.popBackStack()
        }
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            movementViewModel.resetMessages()
        }
    }

    val scrollState = rememberScrollState()

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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = stringResource(R.string.add_movement),
                fontFamily = InterFont,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Botones superiores: Income, Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                MovementTypeButton(
                    text = stringResource(R.string.income),
                    isSelected = selectedMovementType == stringResource(R.string.income),
                    onClick = {
                        selectedMovementType = context.getString(R.string.income)
                        selectedCategory = context.getString(R.string.select_category)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    backgroundColor = Color(0xFF4c6ef5),
                    textColor = Color.White
                )

                MovementTypeButton(
                    text = stringResource(R.string.expense),
                    isSelected = selectedMovementType == context.getString(R.string.expense),
                    onClick = {
                        selectedMovementType = context.getString(R.string.expense)
                        selectedCategory = context.getString(R.string.select_category)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    backgroundColor = Color.White,
                    textColor = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(15.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                    contentDescription = stringResource(R.string.category_background),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Text(
                        text = stringResource(R.string.select_category),
                        fontFamily = InterFont,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = (labelFontSize.value + 4).sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = commonFieldModifier
                    ) {
                        TextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.select_category), color = Color(0xFF868686)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            textStyle = TextStyle(
                                fontFamily = InterFont,
                                color = Color.White,
                                fontSize = labelFontSize
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.9f))
                        ) {
                            currentCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            fontFamily = InterFont,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = labelFontSize,
                                            text = category,
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de cantidad con formato de miles
                    TextField(
                        value = amount,
                        onValueChange = { newValue ->
                            val cleaned = newValue.replace(".", "").replace(",", "")
                            if (cleaned.isEmpty()) {
                                amount = ""
                            } else if (cleaned.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amount = cleaned
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.amount),
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandSeparatorTransformation(),
                        modifier = commonFieldModifier,
                        textStyle = TextStyle(
                            fontFamily = InterFont,
                            color = Color.White,
                            fontSize = labelFontSize
                        ),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = stringResource(R.string.amount),
                                tint = Color.White
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector de Fecha
                    val year = selectedDate.get(Calendar.YEAR)
                    val month = selectedDate.get(Calendar.MONTH)
                    val day = selectedDate.get(Calendar.DAY_OF_MONTH)

                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                            selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth)
                        }, year, month, day
                    )

                    TextField(
                        value = dateFormatter.format(selectedDate.time),
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = stringResource(R.string.date),
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        modifier = commonFieldModifier.clickable { datePickerDialog.show() },
                        textStyle = TextStyle(
                            fontFamily = InterFont,
                            color = Color.White,
                            fontSize = labelFontSize
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = stringResource(R.string.calendar),
                                tint = Color.White
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = stringResource(R.string.select_date),
                                tint = Color.White
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        text = stringResource(R.string.short_description),
                        color = Color.White,
                        fontFamily = InterFont,
                        fontSize = (labelFontSize.value - 2).sp,
                        fontWeight = FontWeight.Thin,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 30.dp)
                    )
                    Spacer(modifier = Modifier.height(15.dp))

                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = {
                            Text(
                                text = stringResource(R.string.add_description),
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        modifier = commonFieldModifier,
                        textStyle = TextStyle(
                            fontFamily = InterFont,
                            color = Color.White,
                            fontSize = labelFontSize
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Botones Save / Cancel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MovementTypeButton(
                            text = stringResource(R.string.save),
                            isSelected = false,
                            onClick = {
                                val amountDouble = amount.toDoubleOrNull()
                                if (amountDouble == null || description.isBlank() || selectedCategory == context.getString(R.string.select_category)) {
                                    Toast.makeText(context, context.getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                                    return@MovementTypeButton
                                }

                                if (selectedMovementType == context.getString(R.string.income)) {
                                    movementViewModel.saveIncome(
                                        amount = amountDouble,
                                        description = description,
                                        category = selectedCategory,
                                        date = selectedDate.time
                                    )
                                } else {
                                    movementViewModel.saveExpense(
                                        amount = amountDouble,
                                        description = description,
                                        category = selectedCategory,
                                        date = selectedDate.time
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            enabled = !uiState.isLoading,
                            backgroundColor = Color(0xFF4c6ef5),
                            textColor = Color.White,
                            isLoading = uiState.isLoading
                        )

                        MovementTypeButton(
                            text = stringResource(R.string.cancel),
                            isSelected = false,
                            onClick = {
                                navController.popBackStack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            backgroundColor = Color.White,
                            textColor = Color.Black
                        )
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
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = if (isSelected) textColor else backgroundColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFont
            )
        }
    }
}