package id.my.ionlinestudio.jadwalsholatindonesia

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || 
                          ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocation) {
            fetchLocationAndPrayers()
        } else {
            locationTextState.value = "Izin lokasi ditolak."
        }
    }

    private var prayerTimesState = mutableStateOf<PrayerTimes?>(null)
    private var locationTextState = mutableStateOf("Mencari lokasi...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val darkColorScheme = darkColorScheme(
                primary = Color(0xFF81C784), // Light Green for dark mode
                secondary = Color(0xFFFFB74D), // Light Orange
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = darkColorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PrayerScreen(
                        prayerTimes = prayerTimesState.value,
                        locationText = locationTextState.value,
                        onSearchCity = { cityName -> searchCity(cityName) },
                        onUseGps = { fetchLocationAndPrayers() }
                    )
                }
            }
        }

        checkPermissions()
    }

    private fun checkPermissions() {
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
            fetchLocationAndPrayers()
        }
    }

    private fun fetchLocationAndPrayers() {
        locationTextState.value = "Mengambil GPS..."
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    calculatePrayers(location.latitude, location.longitude, null)
                } else {
                    locationTextState.value = "Yogyakarta (Default)"
                    calculatePrayers(-7.8014, 110.3644, "Yogyakarta")
                }
            }
        } catch (e: SecurityException) {
            locationTextState.value = "Izin lokasi ditolak."
        }
    }

    private fun searchCity(cityName: String) {
        if (cityName.isBlank()) return
        locationTextState.value = "Mencari $cityName..."
        lifecycleScope.launch {
            var lat = 0.0
            var lng = 0.0
            var found = false
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(cityName, 1)
                    if (!addresses.isNullOrEmpty()) {
                        lat = addresses[0].latitude
                        lng = addresses[0].longitude
                        found = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (found) {
                calculatePrayers(lat, lng, cityName)
            } else {
                locationTextState.value = "Kota tidak ditemukan"
            }
        }
    }

    private fun calculatePrayers(lat: Double, lng: Double, forcedName: String?) {
        lifecycleScope.launch {
            var elevation = 0.0
            var cityName = forcedName ?: "Memuat kota..."
            withContext(Dispatchers.IO) {
                if (forcedName == null) {
                    try {
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "Lokasi Tidak Diketahui"
                        } else {
                            cityName = "Lat: $lat, Lng: $lng"
                        }
                    } catch (e: Exception) {
                        cityName = "Lat: $lat, Lng: $lng"
                    }
                }

                try {
                    val url = URL("https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lng")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().readText()
                        val json = JSONObject(response)
                        val elevArray = json.optJSONArray("elevation")
                        if (elevArray != null && elevArray.length() > 0) {
                            elevation = elevArray.getDouble(0)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            locationTextState.value = "$cityName (${elevation.toInt()} mdpl)"

            var elevationCorrection = 0
            if (elevation > 0) {
                val dipAngle = 0.0347 * Math.sqrt(elevation)
                elevationCorrection = Math.round(dipAngle * 4).toInt()
            }

            val coordinates = Coordinates(lat, lng)
            val params = CalculationMethod.OTHER.parameters
            params.fajrAngle = 20.0
            params.ishaAngle = 18.0
            params.madhab = Madhab.SHAFI
            
            params.adjustments.fajr = 2
            params.adjustments.sunrise = -2 - elevationCorrection
            params.adjustments.dhuhr = 2
            params.adjustments.asr = 2
            params.adjustments.maghrib = 2 + elevationCorrection
            params.adjustments.isha = 2

            val date = DateComponents.from(Date())
            val prayerTimes = PrayerTimes(coordinates, date, params)
            prayerTimesState.value = prayerTimes
            
            // Jadwalkan notifikasi alarm
            AlarmScheduler.scheduleAllPrayers(this@MainActivity, lat, lng)
        }
    }
}

@Composable
fun PrayerScreen(
    prayerTimes: PrayerTimes?, 
    locationText: String,
    onSearchCity: (String) -> Unit,
    onUseGps: () -> Unit
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val currentDate = Date()
    val gregorianFormatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    val gregorianDateString = gregorianFormatter.format(currentDate)
    
    val hijriDateString = try {
        val hijrahDate = HijrahDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))
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
                Icon(Icons.Default.LocationOn, contentDescription = "Gunakan GPS", tint = MaterialTheme.colorScheme.secondary)
            }
            Text(text = "Jadwal Sholat", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            IconButton(onClick = { showSearchDialog = true }) {
                Icon(Icons.Default.Search, contentDescription = "Cari Kota", tint = MaterialTheme.colorScheme.secondary)
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = locationText, style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
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
                    
                    val countdownStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    
                    val prayerName = when(nextPrayer) {
                        Prayer.FAJR -> "Subuh"
                        Prayer.SUNRISE -> "Syuruq"
                        Prayer.DHUHR -> "Dzuhur"
                        Prayer.ASR -> "Ashar"
                        Prayer.MAGHRIB -> "Maghrib"
                        Prayer.ISHA -> "Isya"
                        else -> "Sholat Berikutnya"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3315)) 
                    ) {
                        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), 
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prayerName, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(countdownStr, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            PrayerRow("Subuh", formatter.format(prayerTimes.fajr), nextPrayer == Prayer.FAJR)
            PrayerRow("Syuruq", formatter.format(prayerTimes.sunrise), false)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext) Color(0xFF3E2723) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, color = if(isNext) MaterialTheme.colorScheme.secondary else Color.White)
            Text(text = time, style = MaterialTheme.typography.titleMedium, color = if(isNext) MaterialTheme.colorScheme.secondary else Color.White)
        }
    }
}