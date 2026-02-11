package com.example.coinary.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import android.widget.DatePicker
import android.widget.TimePicker
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.coinary.R
import com.example.coinary.utils.NotificationScheduler
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Screen for creating a new Reminder.
 * Allows the user to select a date, time, category, and amount for a notification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    navController: NavController,
    onBackClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // --- Context & System UI Setup ---
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black

    // Set status bar color
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    // --- Responsive Configuration ---
    // Calculate sizes based on screen dimensions for better responsiveness
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val titleFontSize = if (screenWidth < 360.dp) 22.sp else 26.sp
    val labelFontSize = if (screenWidth < 360.dp) 14.sp else 16.sp
    val buttonHorizontalPadding = if (screenWidth < 360.dp) 12.dp else 20.dp

    // --- State Variables ---
    var bottomButtonSelected by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) } // For Dropdown menu

    // Load categories from resources
    val categories: List<String> = remember {
        context.resources.getStringArray(R.array.categories).toList()
    }
    var selectedCategory by remember {
        mutableStateOf(if (categories.isNotEmpty()) categories[0] else "")
    }

    // Form inputs
    var reminderName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // --- Date & Time Picker Logic ---
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }

    // Date Picker Dialog Setup
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

    // Time Picker Dialog Setup
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

    // --- Common Styles ---
    val commonFieldModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = buttonHorizontalPadding)
        .border(
            width = 1.dp,
            color = Color.White,
            shape = RoundedCornerShape(12.dp)
        )

    val scrollState = rememberScrollState()

    // --- Main Layout ---
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
                .padding(top = 40.dp)
        )

        // Scrollable Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // -- Header (Back & Notification Icons) --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
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

                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // -- Title --
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

            // -- Name Input --
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

            // -- Date & Time Buttons --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = buttonHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Button
                Button(
                    onClick = { datePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedDate.isEmpty()) context.getString(R.string.select_date) else selectedDate,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Time Button
                Button(
                    onClick = { timePickerDialog.show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = "Select Time", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedTime.isEmpty()) context.getString(R.string.select_hour) else selectedTime,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -- Category, Amount & Description Container --
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(15.dp)
            ) {
                // Container Background
                Image(
                    painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                    contentDescription = "Container background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    // Category Label
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

                    // -- Category Dropdown --
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

                    // -- Amount Input (NUMERIC KEYBOARD) --
                    TextField(
                        value = amount,
                        onValueChange = { newText ->
                            // Validate input: Only allow digits and max one decimal point
                            if (newText.all { it.isDigit() || it == '.' } && newText.count { it == '.' } <= 1) {
                                amount = newText
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
                        // Enable Numeric/Decimal Keyboard
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                                contentDescription = "Currency symbol",
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

                    // Description Label
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

                    // -- Description Input --
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

                    // -- Action Buttons (Save / Cancel) --
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MovementTypeButton2(
                            text = context.getString(R.string.save),
                            isSelected = bottomButtonSelected == context.getString(R.string.save),
                            onClick = {
                                bottomButtonSelected = context.getString(R.string.save)
                                // Validate all fields are filled
                                if (
                                    selectedDate.isNotEmpty() &&
                                    selectedTime.isNotEmpty() &&
                                    reminderName.isNotEmpty() &&
                                    amount.isNotEmpty() &&
                                    description.isNotEmpty() &&
                                    selectedCategory.isNotEmpty()
                                ) {
                                    val dateTimeString = "$selectedDate $selectedTime"
                                    // Schedule the notification
                                    NotificationScheduler.scheduleNotification(
                                        context,
                                        dateTimeString,
                                        reminderName,
                                        "$description - ${context.getString(R.string.amount)}: $amount"
                                    )
                                    // Clear form
                                    reminderName = ""
                                    amount = ""
                                    description = ""
                                    selectedDate = ""
                                    selectedTime = ""
                                    selectedCategory = if (categories.isNotEmpty()) categories[0] else ""

                                    Toast.makeText(context, "Recordatorio guardado", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.fill_all_fields),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.height(36.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        MovementTypeButton2(
                            text = context.getString(R.string.cancel),
                            isSelected = bottomButtonSelected == context.getString(R.string.cancel),
                            onClick = {
                                bottomButtonSelected = context.getString(R.string.cancel)
                                // Clear form
                                reminderName = ""
                                amount = ""
                                description = ""
                                selectedDate = ""
                                selectedTime = ""
                                selectedCategory = if (categories.isNotEmpty()) categories[0] else ""
                            },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom styled button component used for Save/Cancel actions.
 */
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