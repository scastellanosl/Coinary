package com.example.coinary.view

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.coinary.R
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

// ============================================================================================
// REGION: NETWORK CONFIGURATION (API)
// ============================================================================================

/**
 * ApiResponse: Data transfer object for the exchange rates API.
 * Maps the raw JSON response containing a map of currency codes and their values.
 */
data class ApiResponse(
    val rates: Map<String, Double>
)

/**
 * CurrencyApi: Retrofit service definition for fetching current exchange rates.
 * Base currency is anchored to COP (Colombian Peso).
 */
interface CurrencyApi {
    @GET("latest/COP")
    suspend fun getRates(): ApiResponse
}

/**
 * RetrofitClient: Singleton provider for the CurrencyApi service.
 */
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

// ============================================================================================
// REGION: VIEWMODEL (I18n Supported)
// ============================================================================================

/**
 * CurrencyViewModel: Handles the state and logic for real-time currency conversion.
 * Orchestrates API calls and manages localized UI states through resource IDs.
 */
class CurrencyViewModel : ViewModel() {

    data class Currency(
        val code: String,
        @StringRes val nameResId: Int,
        val rateToCop: Double,
        val previousRateToCop: Double? = null, // Previous rate for comparison
        val color: Color
    ) {
        /**
         * Calculates the percentage change from the previous rate.
         * Returns null if no previous data is available.
         */
        val changePercent: Double?
            get() = previousRateToCop?.let {
                if (it > 0) ((rateToCop - it) / it) * 100 else null
            }

        val isUp: Boolean
            get() = (changePercent ?: 0.0) > 0

        val isDown: Boolean
            get() = (changePercent ?: 0.0) < 0
    }

    data class CurrencyUiState(
        val baseAmountCop: Double = 0.0,
        val isLoading: Boolean = false,
        @StringRes val errorResId: Int? = null,
        val currencies: List<Currency> = listOf(
            Currency("USD", R.string.currency_usd, 3950.0, null, Color(0xFF4CAF50)),
            Currency("EUR", R.string.currency_eur, 4280.0, null, Color(0xFF2196F3)),
            Currency("GBP", R.string.currency_gbp, 5010.0, null, Color(0xFF9C27B0)),
            Currency("MXN", R.string.currency_mxn, 232.0, null, Color(0xFF009688)),
            Currency("BRL", R.string.currency_brl, 795.0, null, Color(0xFFFFC107)),
            Currency("JPY", R.string.currency_jpy, 26.5, null, Color(0xFFE91E63))
        )
    )

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init {
        fetchLiveRates()
    }

    /**
     * Executes the API request on a background thread.
     * Stores previous rates for comparison and updates the UI state.
     */
    fun fetchLiveRates() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorResId = null)
            try {
                val response = RetrofitClient.api.getRates()
                val rates = response.rates

                // Save previous rates before updating
                val previousCurrencies = _uiState.value.currencies

                val updatedList = listOf(
                    createCurrency("USD", R.string.currency_usd, rates["USD"], Color(0xFF4CAF50), previousCurrencies),
                    createCurrency("EUR", R.string.currency_eur, rates["EUR"], Color(0xFF2196F3), previousCurrencies),
                    createCurrency("GBP", R.string.currency_gbp, rates["GBP"], Color(0xFF9C27B0), previousCurrencies),
                    createCurrency("MXN", R.string.currency_mxn, rates["MXN"], Color(0xFF009688), previousCurrencies),
                    createCurrency("BRL", R.string.currency_brl, rates["BRL"], Color(0xFFFFC107), previousCurrencies),
                    createCurrency("JPY", R.string.currency_jpy, rates["JPY"], Color(0xFFE91E63), previousCurrencies)
                )

                _uiState.value = _uiState.value.copy(
                    currencies = updatedList,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorResId = R.string.currency_error_offline
                )
            }
        }
    }

    private fun createCurrency(
        code: String,
        @StringRes nameResId: Int,
        rateFromApi: Double?,
        color: Color,
        previousCurrencies: List<Currency>
    ): Currency {
        val safeRate = rateFromApi ?: 1.0
        val realRateToCop = 1 / safeRate

        // Find previous rate for comparison
        val previousRate = previousCurrencies.find { it.code == code }?.rateToCop

        return Currency(code, nameResId, realRateToCop, previousRate, color)
    }

    fun updateAmount(amountString: String) {
        val cleanString = amountString.replace(".", "").replace(",", "")
        val amount = cleanString.toDoubleOrNull() ?: 0.0
        _uiState.value = _uiState.value.copy(baseAmountCop = amount)
    }
}

// ============================================================================================
// REGION: UI SCREEN
// ============================================================================================

/**
 * CurrencyScreen: Interface for real-time currency conversion.
 * Features thousand-separator formatting, responsive result list, and currency carousel.
 */
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
            // --- HEADER & NAVIGATION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_desc),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.currency_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                IconButton(onClick = { viewModel.fetchLiveRates() }) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.currency_refresh_desc),
                            tint = Color.White
                        )
                    }
                }
            }

            // USER-FRIENDLY ALERT
            uiState.errorResId?.let { errorId ->
                Text(
                    text = stringResource(errorId),
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- CONVERSION INPUT CARD ---
            Text(
                text = stringResource(R.string.currency_base_label),
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4c6ef5))
                        ) {
                            Text(
                                "COP",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.currency_cop_name),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
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
                        label = {
                            Text(
                                stringResource(R.string.currency_input_label),
                                color = Color.Gray
                            )
                        },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Color(0xFF4c6ef5).copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- REAL-TIME RESULTS LIST ---
            Text(
                text = stringResource(R.string.currency_realtime_label),
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.currencies) { currency ->
                    CurrencyResultItem(
                        currencyItem = currency,
                        baseAmountCop = uiState.baseAmountCop
                    )
                }

                // Spacer before carousel
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.currency_carousel_title),
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // --- CURRENCY CAROUSEL ---
                item {
                    CurrencyCarousel(currencies = uiState.currencies)
                }
            }
        }
    }
}

/**
 * CurrencyResultItem: Individual list item representing a converted currency amount.
 */
@Composable
fun CurrencyResultItem(
    currencyItem: CurrencyViewModel.Currency,
    baseAmountCop: Double
) {
    val convertedAmount = if (currencyItem.rateToCop > 0) {
        baseAmountCop / currencyItem.rateToCop
    } else {
        0.0
    }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(currencyItem.color.copy(alpha = 0.2f))
                ) {
                    Text(
                        currencyItem.code,
                        color = currencyItem.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(currencyItem.nameResId),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "1 ${currencyItem.code} = ${copFormat.format(currencyItem.rateToCop)}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
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
// ============================================================================================
// REGION: CURRENCY CAROUSEL
// ============================================================================================

/**
 * CurrencyCarousel: Horizontal swipeable carousel showing detailed currency information
 * with change indicators compared to the previous day.
 */
@Composable
fun CurrencyCarousel(currencies: List<CurrencyViewModel.Currency>) {
    val pagerState = rememberPagerState(pageCount = { currencies.size })

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { page ->
            CurrencyCarouselCard(currency = currencies[page])
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Page indicators (dots)
        PageIndicator(
            numberOfPages = currencies.size,
            selectedPage = pagerState.currentPage,
            selectedColor = Color(0xFF4c6ef5),
            defaultColor = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * CurrencyCarouselCard: Individual card in the carousel displaying exchange rate
 * and percentage change from the previous day.
 */
@Composable
fun CurrencyCarouselCard(currency: CurrencyViewModel.Currency) {
    val copFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 2
    }

    // Currency names in English
    val currencyNameMap = mapOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "GBP" to "British Pound",
        "MXN" to "Mexican Peso",
        "BRL" to "Brazilian Real",
        "JPY" to "Japanese Yen"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Currency code badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(currency.color.copy(alpha = 0.3f))
            ) {
                Text(
                    currency.code,
                    color = currency.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            // Currency name
            Text(
                text = currencyNameMap[currency.code] ?: currency.code,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )

            // Exchange rate display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "1 ${currency.code} equals to",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    copFormat.format(currency.rateToCop),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp
                )
            }

            // Change indicator
            currency.changePercent?.let { change ->
                val changeColor = when {
                    currency.isDown -> Color(0xFF4CAF50)
                    currency.isUp -> Color(0xFFFF5252)
                    else -> Color.Gray
                }

                val changeText = when {
                    currency.isDown -> "Went down"
                    currency.isUp -> "Went up"
                    else -> "No change"
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(changeColor.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (currency.isDown) {
                                Icons.Default.ArrowDropDown
                            } else {
                                Icons.Default.ArrowDropUp
                            },
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = changeText,
                            color = changeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "${if (change > 0) "+" else ""}%.2f%%".format(change),
                            color = changeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "from previous day",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            } ?: run {
                // When no comparison data is available
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No previous data",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Refresh to compare",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================================
// REGION: PAGE INDICATORS
// ============================================================================================

/**
 * PageIndicator: Animated dot indicators showing current page position.
 */
@Composable
fun PageIndicator(
    numberOfPages: Int,
    selectedPage: Int,
    selectedColor: Color,
    defaultColor: Color,
    defaultRadius: Dp = 6.dp,
    selectedLength: Dp = 20.dp,
    space: Dp = 6.dp,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space),
        modifier = modifier
    ) {
        repeat(numberOfPages) { index ->
            IndicatorDot(
                isSelected = index == selectedPage,
                selectedColor = selectedColor,
                defaultColor = defaultColor,
                defaultRadius = defaultRadius,
                selectedLength = selectedLength
            )
        }
    }
}

/**
 * IndicatorDot: Individual animated dot for page indication.
 */
@Composable
fun IndicatorDot(
    isSelected: Boolean,
    selectedColor: Color,
    defaultColor: Color,
    defaultRadius: Dp,
    selectedLength: Dp
) {
    val width by animateDpAsState(
        targetValue = if (isSelected) selectedLength else defaultRadius,
        label = "width"
    )

    Box(
        modifier = Modifier
            .height(defaultRadius)
            .width(width)
            .clip(CircleShape)
            .background(if (isSelected) selectedColor else defaultColor)
    )
}