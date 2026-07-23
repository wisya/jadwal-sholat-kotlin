package id.my.ionlinestudio.jadwalsholatindonesia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import id.my.ionlinestudio.jadwalsholatindonesia.ui.PrayerIcon
import id.my.ionlinestudio.jadwalsholatindonesia.ui.PrayerViewModel
import id.my.ionlinestudio.jadwalsholatindonesia.ui.QiblaDialogContent
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: PrayerViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                          ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) {
            viewModel.fetchLocationAndPrayers(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadSavedLocation(this)

        setContent {
            JadwalSholatIndonesiaTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WebBackground
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    PrayerScreen(
                        prayerTimes = uiState.prayerTimes,
                        cityName = uiState.cityName,
                        locationText = uiState.locationText,
                        qiblaDegree = uiState.qiblaDegree,
                        isLoading = uiState.isLoading,
                        onSearchCity = { cityName -> viewModel.searchCity(this, cityName) },
                        onUseGps = { checkPermissionsAndFetch() }
                    )
                }
            }
        }

        checkPermissionsAndFetch()
    }

    private fun checkPermissionsAndFetch() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            viewModel.fetchLocationAndPrayers(this)
        }
    }
}

@Composable
fun PrayerScreen(
    prayerTimes: PrayerTimes?,
    cityName: String,
    locationText: String,
    qiblaDegree: Double,
    isLoading: Boolean,
    onSearchCity: (String) -> Unit,
    onUseGps: () -> Unit
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showQiblaDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val currentDate = Date()
    val gregorianFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
    val gregorianDateString = gregorianFormatter.format(currentDate)

    val hijriDateString = try {
        val hijrahDate = HijrahDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag("id-ID"))
        val parts = hijrahDate.format(formatter).split(" ")
        if (parts.size >= 3) {
            val day = parts[0]
            val monthIndex = (hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) - 1).coerceIn(0, 11)
            val year = parts[2]
            val hijriMonths = arrayOf(
                "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
                "Jumadil Awal", "Jumadil Akhir", "Rajab", "Syaban",
                "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"
            )
            "$day ${hijriMonths[monthIndex]} $year H"
        } else {
            hijrahDate.format(formatter) + " H"
        }
    } catch (e: Exception) {
        ""
    }

    // Modal Search City
    if (showSearchDialog) {
        CitySearchDialog(
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            onDismiss = { showSearchDialog = false },
            onSearch = { query ->
                onSearchCity(query)
                showSearchDialog = false
            }
        )
    }

    // Modal Qibla
    if (showQiblaDialog) {
        QiblaDialogContent(
            cityName = cityName,
            qiblaDegree = qiblaDegree,
            onDismiss = { showQiblaDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WebBackground)
    ) {
        // Header Green Container matching web
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = WebHeaderBg,
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jadwal Sholat",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        IconButton(onClick = { onUseGps() }) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "GPS",
                                tint = WebHeaderText
                            )
                        }
                        IconButton(onClick = { showSearchDialog = true }) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Cari Kota",
                                tint = WebHeaderText
                            )
                        }
                        IconButton(onClick = { showQiblaDialog = true }) {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = "Arah Kiblat",
                                tint = WebHeaderText
                            )
                        }
                    }
                }

                // City Name & Dates
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = cityName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = gregorianDateString,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = WebHeaderText
                        )
                        if (hijriDateString.isNotEmpty()) {
                            Text(
                                text = "  |  ",
                                fontSize = 13.sp,
                                color = WebHeaderText.copy(alpha = 0.6f)
                            )
                            Text(
                                text = hijriDateString,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = WebHeaderText
                            )
                        }
                    }

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = WebHeaderText,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = locationText,
                                fontSize = 12.sp,
                                color = WebHeaderText
                            )
                        }
                    }
                }
            }
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (prayerTimes != null) {
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val nextPrayer = prayerTimes.nextPrayer()
                val currentPrayer = prayerTimes.currentPrayer()

                // Countdown Card matching web design
                if (nextPrayer != Prayer.NONE) {
                    val nextTime = prayerTimes.timeForPrayer(nextPrayer)
                    val diff = nextTime.time - currentTimeMillis

                    if (diff > 0) {
                        val hours = (diff / (1000 * 60 * 60)) % 24
                        val minutes = (diff / (1000 * 60)) % 60
                        val seconds = (diff / 1000) % 60

                        val countdownStr = String.format(Locale.US, "-%02d : %02d : %02d", hours, minutes, seconds)

                        val nextPrayerName = when (nextPrayer) {
                            Prayer.FAJR -> "Subuh"
                            Prayer.SUNRISE -> "Syuruq"
                            Prayer.DHUHR -> "Dzuhur"
                            Prayer.ASR -> "Ashar"
                            Prayer.MAGHRIB -> "Maghrib"
                            Prayer.ISHA -> "Isya"
                            else -> "Sholat"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = WebPrimaryContainer),
                            border = BorderStroke(1.dp, WebBorderGreen)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = WebPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = nextPrayerName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WebOnPrimaryContainer
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = countdownStr,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = WebPrimary
                                )
                            }
                        }
                    }
                }

                // Prayer Cards List matching web styling
                val prayerList = listOf(
                    Triple("Subuh", formatter.format(prayerTimes.fajr), currentPrayer == Prayer.FAJR),
                    Triple("Syuruq", formatter.format(prayerTimes.sunrise), currentPrayer == Prayer.SUNRISE),
                    Triple("Dzuhur", formatter.format(prayerTimes.dhuhr), currentPrayer == Prayer.DHUHR),
                    Triple("Ashar", formatter.format(prayerTimes.asr), currentPrayer == Prayer.ASR),
                    Triple("Maghrib", formatter.format(prayerTimes.maghrib), currentPrayer == Prayer.MAGHRIB),
                    Triple("Isya", formatter.format(prayerTimes.isha), currentPrayer == Prayer.ISHA)
                )

                prayerList.forEach { (name, time, isActive) ->
                    WebPrayerCard(name = name, time = time, isActive = isActive)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WebPrimary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WebPrayerCard(name: String, time: String, isActive: Boolean) {
    val cardBackground = if (isActive) WebPrimaryContainer else WebSurface
    val cardBorder = if (isActive) BorderStroke(1.dp, WebBorderGreen) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prayer Icon
            PrayerIcon(
                prayerKey = name,
                modifier = Modifier.size(26.dp),
                tint = WebSecondary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Prayer Name
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) WebOnPrimaryContainer else WebTextPrimary
            )

            if (isActive) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = WebPrimary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SEKARANG",
                        color = WebOnPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Time
            Text(
                text = time,
                fontSize = 20.sp,
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isActive) WebPrimary else WebTextPrimary
            )
        }
    }
}

@Composable
fun CitySearchDialog(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit
) {
    val popularCities = listOf("Jakarta", "Bandung", "Surabaya", "Yogyakarta", "Medan", "Makassar")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WebSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = WebPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pilih Kota", color = WebTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Ketik nama kota (misal: Bandung)", color = WebTextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WebPrimary,
                        unfocusedBorderColor = WebSurfaceVariant,
                        focusedTextColor = WebTextPrimary,
                        unfocusedTextColor = WebTextPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(searchQuery) })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Kota Populer:", fontSize = 13.sp, color = WebTextSecondary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(popularCities) { city ->
                        Surface(
                            color = WebSurfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { onSearch(city) }
                        ) {
                            Text(
                                text = city,
                                color = WebTextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSearch(searchQuery) },
                colors = ButtonDefaults.buttonColors(containerColor = WebPrimary, contentColor = WebOnPrimary)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cari")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = WebTextSecondary)
            }
        }
    )
}
