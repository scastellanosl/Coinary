package com.example.coinary.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
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
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ReminderScreen: UI for creating and scheduling new financial reminders.
 * Allows users to set a title, date, time, category, amount, and description.
 * Utilizes [NotificationScheduler] to set system alarms.
 *
 * @param navController Navigation controller for screen transitions.
 * @param onBackClick Callback for back navigation (optional).
 * @param onLogout Callback for logout (optional).
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val statusBarColor = Color.Black

    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    // --- Responsive Configuration ---
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val titleFontSize = if (screenWidth < 360.dp) 22.sp else 26.sp
    val labelFontSize = if (screenWidth < 360.dp) 14.sp else 16.sp
    val buttonHorizontalPadding = if (screenWidth < 360.dp) 12.dp else 20.dp

    // --- State Variables ---
    var expanded by remember { mutableStateOf(false) } // Dropdown state

    // Load categories
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

    // Date Picker Dialog
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            selectedDate = format.format(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Time Picker Dialog
    val timePickerDialog = TimePickerDialog(
        context,
        { _: TimePicker, hourOfDay: Int, minute: Int ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            selectedTime = format.format(calendar.time)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false // 12-hour format (AM/PM)
    )

    // --- Styling Modifiers ---
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
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                // -- Header --
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

                Spacer(modifier = Modifier.height(40.dp))

                // -- Title --
                Text(
                    text = stringResource(R.string.create_reminder),
                    fontFamily = InterFont, // Ensure InterFont is defined in your theme
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
                            text = stringResource(R.string.name_reminder),
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
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedDate.isEmpty()) stringResource(R.string.select_date) else selectedDate,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Time Button
                    Button(
                        onClick = { timePickerDialog.show() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = "Select Time", tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedTime.isEmpty()) stringResource(R.string.select_hour) else selectedTime,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -- Details Container (Category, Amount, Description) --
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(15.dp)
                ) {
                    // Container Background
                    Image(
                        painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(), // Changed to fillMaxWidth to prevent layout issues
                        contentScale = ContentScale.FillWidth
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        // Category Label
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

                        // -- Category Dropdown --
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = commonFieldModifier
                        ) {
                            TextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                textStyle = TextStyle(
                                    fontFamily = InterFont,
                                    fontSize = labelFontSize,
                                    color = Color.White
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(Color.DarkGray)
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = category,
                                                fontFamily = InterFont,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = labelFontSize,
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

                        // -- Amount Input --
                        TextField(
                            value = amount,
                            onValueChange = { newText ->
                                if (newText.all { it.isDigit() || it == '.' } && newText.count { it == '.' } <= 1) {
                                    amount = newText
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
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // Description Label
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

                        // -- Description Input --
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
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // -- Action Buttons --
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Save Button
                            MovementTypeButton2(
                                text = stringResource(R.string.save),
                                isSelected = true, // Highlight primary action
                                onClick = {
                                    if (selectedDate.isNotEmpty() && selectedTime.isNotEmpty() &&
                                        reminderName.isNotEmpty() && amount.isNotEmpty() &&
                                        description.isNotEmpty() && selectedCategory.isNotEmpty()
                                    ) {
                                        val dateTimeString = "$selectedDate $selectedTime"
                                        val fullMessage = "$description - ${context.getString(R.string.amount)}: $amount"

                                        NotificationScheduler.scheduleNotification(
                                            context,
                                            dateTimeString,
                                            reminderName,
                                            fullMessage
                                        )

                                        // Reset Form
                                        reminderName = ""
                                        amount = ""
                                        description = ""
                                        selectedDate = ""
                                        selectedTime = ""
                                        selectedCategory = if (categories.isNotEmpty()) categories[0] else ""

                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Reminder saved successfully")
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Please fill all fields")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Cancel Button
                            MovementTypeButton2(
                                text = stringResource(R.string.cancel),
                                isSelected = false,
                                onClick = {
                                    reminderName = ""
                                    amount = ""
                                    description = ""
                                    selectedDate = ""
                                    selectedTime = ""
                                    selectedCategory = if (categories.isNotEmpty()) categories[0] else ""
                                    navController.popBackStack()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * MovementTypeButton2: Custom button component for form actions.
 * Supports selected state styling (Blue fill vs Transparent with border).
 *
 * @param text Button label.
 * @param isSelected Determines visual style (Primary vs Secondary).
 * @param onClick Action to perform on click.
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
        modifier = modifier.height(48.dp),
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