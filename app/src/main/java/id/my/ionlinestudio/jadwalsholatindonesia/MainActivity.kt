package id.my.ionlinestudio.jadwalsholatindonesia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.batoulapps.adhan.Prayer
import com.batoulapps.adhan.PrayerTimes
import id.my.ionlinestudio.jadwalsholatindonesia.ui.PrayerViewModel
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.CardGreenContainer
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.CardHighlightNext
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.JadwalSholatIndonesiaTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.*

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

        // Load saved location offline first
        viewModel.loadSavedLocation(this)

        setContent {
            JadwalSholatIndonesiaTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    PrayerScreen(
                        prayerTimes = uiState.prayerTimes,
                        locationText = uiState.locationText,
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
    locationText: String,
    isLoading: Boolean,
    onSearchCity: (String) -> Unit,
    onUseGps: () -> Unit
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSearchDialog by remember { mutableStateOf(false) }
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
        hijrahDate.format(formatter) + " H"
    } catch (e: Exception) {
        ""
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Cari Lokasi Kota", color = MaterialTheme.colorScheme.primary) },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Masukkan nama kota...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearchCity(searchQuery)
                        showSearchDialog = false
                    })
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSearchCity(searchQuery)
                    showSearchDialog = false
                }) {
                    Text("Cari", color = MaterialTheme.colorScheme.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Batal", color = Color.Gray)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onUseGps() }) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Gunakan GPS",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = "Jadwal Sholat",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = { showSearchDialog = true }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Cari Kota",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = locationText, style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
            if (isLoading) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = gregorianDateString, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        if (hijriDateString.isNotEmpty()) {
            Text(text = hijriDateString, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (prayerTimes != null) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val nextPrayer = prayerTimes.nextPrayer()

            if (nextPrayer != Prayer.NONE) {
                val nextTime = prayerTimes.timeForPrayer(nextPrayer)
                val diff = nextTime.time - currentTimeMillis

                if (diff > 0) {
                    val hours = (diff / (1000 * 60 * 60)) % 24
                    val minutes = (diff / (1000 * 60)) % 60
                    val seconds = (diff / 1000) % 60

                    val countdownStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

                    val prayerName = when (nextPrayer) {
                        Prayer.FAJR -> "Subuh"
                        Prayer.SUNRISE -> "Syuruq"
                        Prayer.DHUHR -> "Dzuhur"
                        Prayer.ASR -> "Ashar"
                        Prayer.MAGHRIB -> "Maghrib"
                        Prayer.ISHA -> "Isya"
                        else -> "Sholat Berikutnya"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardGreenContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏳", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prayerName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                countdownStr,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            PrayerRow("Subuh", formatter.format(prayerTimes.fajr), nextPrayer == Prayer.FAJR)
            PrayerRow("Syuruq", formatter.format(prayerTimes.sunrise), nextPrayer == Prayer.SUNRISE)
            PrayerRow("Dzuhur", formatter.format(prayerTimes.dhuhr), nextPrayer == Prayer.DHUHR)
            PrayerRow("Ashar", formatter.format(prayerTimes.asr), nextPrayer == Prayer.ASR)
            PrayerRow("Maghrib", formatter.format(prayerTimes.maghrib), nextPrayer == Prayer.MAGHRIB)
            PrayerRow("Isya", formatter.format(prayerTimes.isha), nextPrayer == Prayer.ISHA)
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PrayerRow(name: String, time: String, isNext: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) CardHighlightNext else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isNext) MaterialTheme.colorScheme.secondary else Color.White
            )
            Text(
                text = time,
                style = MaterialTheme.typography.titleMedium,
                color = if (isNext) MaterialTheme.colorScheme.secondary else Color.White
            )
        }
    }
}