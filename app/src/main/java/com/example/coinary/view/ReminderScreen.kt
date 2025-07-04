package com.example.coinary.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.utils.NotificationScheduler
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    navController: NavController,
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    val context = LocalContext.current

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

    var selectedMovementType by remember { mutableStateOf("Income") }
    var bottomButtonSelected by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }
    val categories: List<String> = remember {
        context.resources.getStringArray(R.array.categories).toList()
    }
    var selectedCategory by remember {
        mutableStateOf(
            if (categories.isNotEmpty()) {
                categories[0]
            } else {
                ""
            }
        )
    }
    var reminderName by remember { mutableStateOf("") }
    // Este 'amount' es para el valor monetario
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // --- NUEVAS VARIABLES DE ESTADO PARA FECHA Y HORA ---
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    // Date Picker Dialog
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            selectedDate = format.format(calendar.time)
        }, year, month, day
    )

    // Time Picker Dialog
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = TimePickerDialog(
        context,
        { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            selectedTime = format.format(calendar.time)
        }, hour, minute, false
    )


    val commonFieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = buttonHorizontalPadding)
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        )

    // NUEVO: Estado de scroll
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
                text = context.getString(R.string.create_reminder),
                fontFamily = InterFont,
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = reminderName,
                onValueChange = { reminderName = it },
                label = {
                    Text(
                        text = context.getString(R.string.name_reminder),
                        fontFamily = InterFont,
                        fontWeight = FontWeight.Normal,
                        fontSize = labelFontSize,
                        color = Color(0xFF868686)
                    )
                },
                singleLine = true,
                modifier = commonFieldModifier.fillMaxWidth(0.95f),
                textStyle = TextStyle(
                    fontFamily = InterFont,
                    color = Color.White,
                    fontSize = labelFontSize
                ),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = buttonHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (selectedDate.isEmpty()) "Seleccionar Fecha" else selectedDate, color = Color.White)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { timePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = "Select Time", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (selectedTime.isEmpty()) "Seleccionar Hora" else selectedTime, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))


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
                        Text(
                            text = selectedCategory,
                            color = Color.White,
                            fontFamily = InterFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = labelFontSize,
                            modifier = Modifier
                                .menuAnchor()
                                .padding(12.dp)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
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

                    TextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = {
                            Text(
                                text = context.getString(R.string.amount), // Asegúrate de tener este String
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
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(30.dp))

                    Text(
                        text = context.getString(R.string.short_description), // Asegúrate de tener este String
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
                                text = context.getString(R.string.add_description), // Asegúrate de tener este String
                                fontFamily = InterFont,
                                fontWeight = FontWeight.Normal,
                                fontSize = labelFontSize,
                                color = Color(0xFF868686)
                            )
                        },
                        singleLine = true,
                        modifier = commonFieldModifier.padding(vertical = 30.dp),
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
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MovementTypeButton2(
                            text = context.getString(R.string.save),
                            isSelected = bottomButtonSelected == "Save",
                            onClick = {
                                bottomButtonSelected = "Save"
                                if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty() && reminderName.isNotEmpty()) {
                                    val dateTimeString = "$selectedDate $selectedTime"
                                    NotificationScheduler.scheduleNotification(
                                        context,
                                        dateTimeString,
                                        reminderName,
                                        description
                                    )
                                } else {
                                    // Toast.makeText(context, "Por favor, completa la fecha, hora y nombre del recordatorio.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.height(36.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        MovementTypeButton2(
                            context.getString(R.string.cancel),
                            isSelected = bottomButtonSelected == "Cancel",
                            onClick = { bottomButtonSelected = "Cancel" },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun MovementTypeButton2(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4c6ef5) else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(Color.Gray, Color.Gray))
        ) else null,
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
