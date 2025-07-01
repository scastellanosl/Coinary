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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.viewmodel.MovementViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovementScreen(
    navController: NavController,
    movementViewModel: MovementViewModel = viewModel(), // Inyecta el ViewModel
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
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
    val buttonHeight = if (screenHeight < 600.dp) 32.dp else 36.dp
    val buttonHorizontalPadding = if (screenWidth < 360.dp) 12.dp else 20.dp

    var selectedMovementType by remember { mutableStateOf("Income") } // "Income" o "Expense"
    // var bottomButtonSelected by remember { mutableStateOf<String?>(null) } // No es necesario para el guardado

    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Categorías específicas para ingresos y gastos (mejor que R.array.categories genéricas)
    val incomeCategories = remember { listOf("Salario", "Regalo", "Ventas", "Inversión", "Otros Ingresos") }
    val expenseCategories = remember { listOf("Comida", "Transporte", "Vivienda", "Entretenimiento", "Servicios", "Compras", "Salud", "Educación", "Otros Gastos") }
    val currentCategories = if (selectedMovementType == "Income") incomeCategories else expenseCategories

    var selectedCategory by remember { mutableStateOf("Selecciona Categoría") } // Valor inicial

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) } // Para la fecha

    val commonFieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = buttonHorizontalPadding)
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        )

    // Formateador de fecha
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Observar el estado del ViewModel
    val uiState by movementViewModel.uiState.collectAsState()
    LaunchedEffect(uiState) {
        uiState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // Limpiar campos después de guardar exitosamente
            amount = ""
            description = ""
            selectedCategory = "Selecciona Categoría"
            selectedDate = Calendar.getInstance()
            movementViewModel.resetMessages()
            // Opcional: Navegar hacia atrás o a otra pantalla
            navController.popBackStack()
        }
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            movementViewModel.resetMessages()
        }
    }

    // Estado del scroll
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
                .fillMaxSize() // Mantener fillMaxSize para que el Column tome todo el espacio
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState) // <-- Aplicar el modificador de scroll aquí
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
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = context.getString(R.string.add_movement),
                fontFamily = InterFont, // Asegúrate de que InterFont esté disponible
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                MovementTypeButton(
                    text = context.getString(R.string.income),
                    isSelected = selectedMovementType == "Income",
                    onClick = {
                        selectedMovementType = "Income"
                        selectedCategory = "Selecciona Categoría" // Reset category when type changes
                    },
                    modifier = Modifier
                        .width(130.dp)
                        .height(buttonHeight)
                )

                MovementTypeButton(
                    text = context.getString(R.string.expense),
                    isSelected = selectedMovementType == "Expense",
                    onClick = {
                        selectedMovementType = "Expense"
                        selectedCategory = "Selecciona Categoría" // Reset category when type changes
                    },
                    modifier = Modifier
                        .width(130.dp)
                        .height(buttonHeight)
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(15.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                    contentDescription = "Fondo contenedor categoria",
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
                        text = context.getString(R.string.select_category),
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
                            onValueChange = {}, // No permite edición directa
                            readOnly = true,
                            label = { Text("Categoría", color = Color(0xFF868686)) }, // Etiqueta para TextField
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(), // Esencial para el comportamiento del menú
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
                                unfocusedTextColor = Color.White, // Color del texto cuando no está enfocado
                                focusedTextColor = Color.White // Color del texto cuando está enfocado
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.DarkGray.copy(alpha = 0.9f)) // Fondo para el menú
                        ) {
                            currentCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            fontFamily = InterFont,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = labelFontSize,
                                            text = category,
                                            color = Color.White // Color del texto en el menú
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

                    TextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Permite solo números y un punto decimal
                            if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                amount = newValue
                            }
                        },
                        label = {
                            Text(
                                text = context.getString(R.string.amount),
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Importante para el teclado numérico
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
                                contentDescription = "Dollar symbol",
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
                    Spacer(modifier = Modifier.height(16.dp)) // Espacio después del monto

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
                        onValueChange = {}, // Read-only
                        readOnly = true,
                        label = {
                            Text(
                                text = "Fecha",
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        modifier = commonFieldModifier.clickable { datePickerDialog.show() }, // Hace que todo el TextField sea clickeable
                        textStyle = TextStyle(
                            fontFamily = InterFont,
                            color = Color.White,
                            fontSize = labelFontSize
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar icon",
                                tint = Color.White
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Seleccionar fecha",
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
                        text = context.getString(R.string.short_description),
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
                                text = context.getString(R.string.add_description),
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        modifier = commonFieldModifier, // Elimina el padding vertical extra aquí
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
                    Spacer(modifier = Modifier.height(15.dp))

                    // Botones inferiores independientes (Save / Cancel)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón de Guardar
                        MovementTypeButton(
                            text = context.getString(R.string.save),
                            isSelected = uiState.isLoading, // Opcional: indicar que está cargando
                            onClick = {
                                val amountDouble = amount.toDoubleOrNull()
                                if (amountDouble == null || description.isBlank() || selectedCategory == "Selecciona Categoría") {
                                    Toast.makeText(context, "Por favor, completa todos los campos válidos.", Toast.LENGTH_SHORT).show()
                                    return@MovementTypeButton
                                }

                                if (selectedMovementType == "Income") {
                                    movementViewModel.saveIncome(
                                        amount = amountDouble,
                                        description = description,
                                        category = selectedCategory,
                                        date = selectedDate.time // Usa .time para obtener Date de Calendar
                                    )
                                } else { // Es un Gasto
                                    movementViewModel.saveExpense(
                                        amount = amountDouble,
                                        description = description,
                                        category = selectedCategory,
                                        date = selectedDate.time
                                    )
                                }
                            },
                            modifier = Modifier.height(36.dp),
                            enabled = !uiState.isLoading // Deshabilita mientras carga
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Botón de Cancelar
                        MovementTypeButton(
                            text = context.getString(R.string.cancel),
                            isSelected = false, // Nunca estará seleccionado de esta manera
                            onClick = {
                                navController.popBackStack() // Simplemente vuelve a la pantalla anterior
                            },
                            modifier = Modifier.height(36.dp)
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
    enabled: Boolean = true // Añadir parámetro de habilitación
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4c6ef5) else Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.5f) // Color cuando está deshabilitado
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(Color.Gray, Color.Gray))
        ) else null,
        contentPadding = PaddingValues(horizontal = 20.dp),
        enabled = enabled // Usar el parámetro enabled
    ) {
        if (text == "Guardar" && !enabled) { // Muestra CircularProgressIndicator solo para "Guardar" cuando está cargando
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

