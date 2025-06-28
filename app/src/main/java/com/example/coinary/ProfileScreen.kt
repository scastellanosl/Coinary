package com.example.coinary

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun ProfileScreen(
    navController: NavController, onLogout: () -> Unit
) {

    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color.Black
    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }

    val screenHeight =
        LocalConfiguration.current.screenHeightDp.dp //Para aplicar responsividad, conociendo las dimensiones del dispositivo
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val user = googleAuthClient.getSignedInUser()
    var bottomButtonSelected by remember { mutableStateOf<String?>(null) }

    var hour by remember { mutableStateOf(18) } // Hora predterminada para la fecha al abrir el aplicativo, si son las 6pm se enviaría la notificación
    var minute by remember { mutableStateOf(0) }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                hour = selectedHour
                minute = selectedMinute
            },
            hour,
            minute,
            true      // false = formato 12h, true = formato 24h
        )
    }

    var currentpass by remember { mutableStateOf("") }
    var newpass by remember { mutableStateOf("") }
    var confirmnewpass by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
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

            Text(text = context.getString(R.string.profile_configuration), color = Color.White)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 3.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { navController.navigate("notifications") }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier
                            .size(35.dp)
                            .padding(top = 5.dp)
                    )
                }
            }


        }

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.darker_background),
                contentDescription = "User icon",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Box(
                    modifier = Modifier
                        .offset(y = (screenHeight * 0.025f))
                        .fillMaxWidth(0.25f)
                        .aspectRatio(1f)
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.user_icon),
                        contentDescription = "User icon",
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }

                Text(
                    user?.username ?: "User",
                    fontWeight = FontWeight.Bold,
                    fontSize = (screenWidthDp * 0.042f).sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = screenHeight * 0.032f)
                        .fillMaxWidth(1f),
                    textAlign = TextAlign.Center
                )


                Column() {
                    Box(modifier = Modifier.fillMaxWidth()) {

                        Image(
                            painter = painterResource(id = R.drawable.fondo_contenedor_categoria),
                            contentDescription = "background for more info", modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = screenWidth * 0.048f)
                                .padding(top = screenHeight * 0.012f),
                            contentScale = ContentScale.Crop
                        )
                        Column {

                            Text(
                                text = context.getString(R.string.your_info),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = (screenWidthDp * 0.06f).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(top = screenHeight * 0.038f)
                                    .fillMaxWidth(1f)
                            )

                            Text(
                                text = context.getString(R.string.email),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = (screenWidthDp * 0.045f).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(top = screenHeight * 0.012f)
                                    .fillMaxWidth(1f)
                            )

                            Text(
                                user?.userId ?: "mailusuario",
                                color = Color.White,
                                fontWeight = FontWeight.Thin,
                                fontSize = (screenWidthDp * 0.04f).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(top = screenHeight * 0.004f)
                                    .fillMaxWidth(1f)
                            )

                            Text(
                                text = context.getString(R.string.hour),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = (screenWidthDp * 0.048f).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(top = screenHeight * 0.02f)
                                    .fillMaxWidth(1f)
                            )

                            // Definición de la hora

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = (screenHeightDp * 0.01f).dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // HORA
                                Box(
                                    modifier = Modifier
                                        .size(screenHeight * 0.06f)
                                        .clickable { timePickerDialog.show() }
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.button_background),
                                        contentDescription = "Hour background",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = "%02d".format(if (hour % 12 == 0) 12 else hour % 12),
                                        color = Color.White,
                                        fontSize = (screenWidthDp * 0.052f).sp, // Reducido
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                // DOS PUNTOS
                                Text(
                                    text = ":",
                                    color = Color.White,
                                    fontSize = (screenWidthDp * 0.105f).sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                )

                                // MINUTOS
                                Box(
                                    modifier = Modifier
                                        .size(screenHeight * 0.06f)
                                        .clickable { timePickerDialog.show() }
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.button_background),
                                        contentDescription = "Minute background",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = "%02d".format(minute),
                                        color = Color.White,
                                        fontSize = (screenWidthDp * 0.052f).sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.width((screenWidth * 0.022f)))

                                // AM / PM

                                Box(
                                    modifier = Modifier
                                        .size(screenHeight * 0.06f)
                                        .clickable { timePickerDialog.show() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.button_hour),
                                        contentDescription = "AM/PM background",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Text(
                                        text = if (hour < 12) "AM" else "PM",
                                        color = Color.White,
                                        fontSize = (screenWidthDp * 0.052f).sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = context.getString(R.string.change_password),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = (screenWidthDp * 0.042f).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(
                                        top = screenHeight * 0.02f,
                                        bottom = screenHeight * 0.012f
                                    )
                                    .fillMaxWidth(1f)
                            )


                            /*

                            var currentpass by remember { mutableStateOf("") }
                            var newpass by remember { mutableStateOf("") }
                            var confirmnewpass by remember { mutableStateOf("") }

                            */

                            Column(
                                modifier = Modifier
                                    .padding(bottom = screenHeight * 0.012f)
                                    .fillMaxWidth()
                            ) {

                                // Current password
                                OutlinedTextField(
                                    value = currentpass,
                                    onValueChange = { currentpass = it },
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            context.getString(R.string.current_password),
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = (screenWidthDp * 0.0275f).sp,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = (screenWidthDp * 0.05f).sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .width(screenWidth * 0.78f)
                                        .align(Alignment.CenterHorizontally)
                                        .height(screenHeight * 0.058f)
                                )

                            }

                            Column(
                                modifier = Modifier
                                    .padding(bottom = screenHeight * 0.004f)
                                    .fillMaxWidth()
                            ) {

                                // New password
                                OutlinedTextField(
                                    value = newpass,
                                    onValueChange = { newpass = it },
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            text = context.getString(R.string.new_password),
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = (screenWidthDp * 0.0275f).sp,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = (screenWidthDp * 0.05f).sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .width(screenWidth * 0.78f)
                                        .align(Alignment.CenterHorizontally)
                                        .height(screenHeight * 0.058f)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .padding(bottom = screenHeight * 0.012f)
                                    .fillMaxWidth()
                            ) {

                                // Password validation
                                OutlinedTextField(
                                    value = confirmnewpass,
                                    onValueChange = { confirmnewpass = it },
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    placeholder = {
                                        Text(
                                            context.getString(R.string.confirm_password),
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = (screenWidthDp * 0.0275f).sp,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = (screenWidthDp * 0.05f).sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier
                                        .width(screenWidth * 0.78f)
                                        .align(Alignment.CenterHorizontally)
                                        .height(screenHeight * 0.058f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { bottomButtonSelected = "Save changes" },
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4D54BF),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = context.getString(R.string.save_changes),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Botón cierre de sesión
                                Button(
                                    onClick = {
                                        bottomButtonSelected = "Close session"
                                        coroutineScope.launch {
                                            googleAuthClient.signOut()
                                            onLogout()
                                        }
                                    },
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFBF4D4D),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = context.getString(R.string.logout),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                        }

                    }
                }

            }

        }

    }
}
