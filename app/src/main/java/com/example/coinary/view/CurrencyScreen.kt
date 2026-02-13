package com.example.coinary.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.utils.ThousandSeparatorTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.text.NumberFormat
import java.util.Locale

// --- 1. CONFIGURACIÓN DE RED (API) ---

// Modelo de datos que viene de internet
data class ApiResponse(
    val rates: Map<String, Double>
)

// Interfaz para pedir los datos
interface CurrencyApi {
    // Pedimos las tasas basadas en COP (Peso Colombiano)
    @GET("latest/COP")
    suspend fun getRates(): ApiResponse
}

// Objeto para crear la conexión
object RetrofitClient {
    private const val BASE_URL = "https://open.er-api.com/v6/"

    val api: CurrencyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApi::class.java)
    }
}

// --- 2. VIEWMODEL (Lógica Real) ---
class CurrencyViewModel : ViewModel() {

    data class Currency(
        val code: String,
        val name: String,
        val rateToCop: Double,
        val color: Color
    )

    data class CurrencyUiState(
        val baseAmountCop: Double = 0.0,
        val isLoading: Boolean = false, // Para mostrar cargando
        val errorMessage: String? = null,
        val currencies: List<Currency> = listOf(
            // Valores iniciales (por si no hay internet)
            Currency("USD", "Dólar Estadounidense", 3950.0, Color(0xFF4CAF50)),
            Currency("EUR", "Euro", 4280.0, Color(0xFF2196F3)),
            Currency("GBP", "Libra Esterlina", 5010.0, Color(0xFF9C27B0)),
            Currency("MXN", "Peso Mexicano", 232.0, Color(0xFF009688)),
            Currency("BRL", "Real Brasileño", 795.0, Color(0xFFFFC107)),
            Currency("JPY", "Yen Japonés", 26.5, Color(0xFFE91E63))
        )
    )

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init {
        // Al iniciar, buscamos los datos en internet
        fetchLiveRates()
    }

    fun fetchLiveRates() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // 1. Llamamos a la API
                val response = RetrofitClient.api.getRates()

                // 2. Calculamos las tasas
                // La API nos dice: 1 COP = 0.00025 USD
                // Nosotros queremos: 1 USD = X COP.  (X = 1 / 0.00025)

                val rates = response.rates

                val updatedList = listOf(
                    createCurrency("USD", "Dólar Estadounidense", rates["USD"], Color(0xFF4CAF50)),
                    createCurrency("EUR", "Euro", rates["EUR"], Color(0xFF2196F3)),
                    createCurrency("GBP", "Libra Esterlina", rates["GBP"], Color(0xFF9C27B0)),
                    createCurrency("MXN", "Peso Mexicano", rates["MXN"], Color(0xFF009688)),
                    createCurrency("BRL", "Real Brasileño", rates["BRL"], Color(0xFFFFC107)),
                    createCurrency("JPY", "Yen Japonés", rates["JPY"], Color(0xFFE91E63))
                )

                _uiState.value = _uiState.value.copy(
                    currencies = updatedList,
                    isLoading = false
                )

            } catch (e: Exception) {
                // Si falla (sin internet), dejamos los valores por defecto y mostramos error
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Sin conexión. Usando valores aproximados."
                )
            }
        }
    }

    private fun createCurrency(code: String, name: String, rateFromApi: Double?, color: Color): Currency {
        // Si la API devolvió null, usamos 1.0 para evitar división por cero
        val safeRate = rateFromApi ?: 1.0
        // Invertimos la tasa: Si 1 COP = 0.00025 USD -> 1 USD = 1/0.00025 COP
        val realRateToCop = 1 / safeRate

        return Currency(code, name, realRateToCop, color)
    }

    fun updateAmount(amountString: String) {
        val cleanString = amountString.replace(".", "").replace(",", "")
        val amount = cleanString.toDoubleOrNull() ?: 0.0
        _uiState.value = _uiState.value.copy(baseAmountCop = amount)
    }
}

// --- 3. PANTALLA UI ---
@Composable
fun CurrencyScreen(
    navController: NavController,
    onBackClick: () -> Unit = { navController.popBackStack() }
) {
    val viewModel: CurrencyViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Text(
                        text = "Divisas Hoy",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Botón de refrescar
                IconButton(onClick = { viewModel.fetchLiveRates() }) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color.White)
                    }
                }
            }

            // Mensaje de error si no hay internet
            uiState.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- INPUT CARD ---
            Text("Tu moneda base (COP)", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF4c6ef5))
                        ) {
                            Text("COP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Peso Colombiano", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = inputText,
                        onValueChange = {
                            val cleaned = it.replace(".", "").replace(",", "")
                            if (cleaned.all { char -> char.isDigit() }) {
                                inputText = cleaned
                                viewModel.updateAmount(cleaned)
                            }
                        },
                        label = { Text("Monto a convertir", color = Color.Gray) },
                        prefix = { Text("$ ", color = Color.White) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = ThousandSeparatorTransformation(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2B2B40),
                            unfocusedContainerColor = Color(0xFF2B2B40),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF4c6ef5).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Conversión en tiempo real", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.currencies) { currency ->
                    CurrencyResultItem(currencyItem = currency, baseAmountCop = uiState.baseAmountCop)
                }
            }
        }
    }
}

@Composable
fun CurrencyResultItem(
    currencyItem: CurrencyViewModel.Currency,
    baseAmountCop: Double
) {
    val convertedAmount = if (currencyItem.rateToCop > 0) baseAmountCop / currencyItem.rateToCop else 0.0

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 2
        currency = java.util.Currency.getInstance(currencyItem.code)
    }

    val copFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(currencyItem.color.copy(alpha = 0.2f))
                ) {
                    Text(currencyItem.code, color = currencyItem.color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(currencyItem.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    // Muestra el precio del día
                    Text("1 ${currencyItem.code} = ${copFormat.format(currencyItem.rateToCop)}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Text(
                text = currencyFormat.format(convertedAmount),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}