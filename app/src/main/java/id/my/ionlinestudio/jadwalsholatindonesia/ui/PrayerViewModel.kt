package id.my.ionlinestudio.jadwalsholatindonesia.ui

import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.PrayerTimes
import com.google.android.gms.location.LocationServices
import id.my.ionlinestudio.jadwalsholatindonesia.AlarmScheduler
import id.my.ionlinestudio.jadwalsholatindonesia.JadwalSholatWidget
import id.my.ionlinestudio.jadwalsholatindonesia.data.LocationPreferences
import id.my.ionlinestudio.jadwalsholatindonesia.data.PrayerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class PrayerUiState(
    val prayerTimes: PrayerTimes? = null,
    val locationText: String = "Mencari lokasi...",
    val isLoading: Boolean = false
)

class PrayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    fun loadSavedLocation(context: Context) {
        val locationPrefs = LocationPreferences(context)
        val userLocation = locationPrefs.getLocation()

        val prayerTimes = PrayerCalculator.calculatePrayerTimes(
            latitude = userLocation.latitude,
            longitude = userLocation.longitude,
            elevation = userLocation.elevation
        )

        _uiState.update {
            it.copy(
                prayerTimes = prayerTimes,
                locationText = "${userLocation.cityName} (${userLocation.elevation.toInt()} mdpl)",
                isLoading = false
            )
        }
    }

    fun fetchLocationAndPrayers(context: Context) {
        _uiState.update { it.copy(locationText = "Mengambil GPS...", isLoading = true) }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    calculatePrayers(context, location.latitude, location.longitude, null)
                } else {
                    val saved = LocationPreferences(context).getLocation()
                    calculatePrayers(context, saved.latitude, saved.longitude, saved.cityName)
                }
            }.addOnFailureListener {
                val saved = LocationPreferences(context).getLocation()
                calculatePrayers(context, saved.latitude, saved.longitude, saved.cityName)
            }
        } catch (e: SecurityException) {
            _uiState.update { it.copy(locationText = "Izin lokasi ditolak.", isLoading = false) }
        }
    }

    fun searchCity(context: Context, cityName: String) {
        if (cityName.isBlank()) return
        _uiState.update { it.copy(locationText = "Mencari $cityName...", isLoading = true) }
        viewModelScope.launch {
            var lat = 0.0
            var lng = 0.0
            var found = false
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
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
                calculatePrayers(context, lat, lng, cityName)
            } else {
                _uiState.update { it.copy(locationText = "Kota tidak ditemukan", isLoading = false) }
            }
        }
    }

    private fun calculatePrayers(context: Context, lat: Double, lng: Double, forcedName: String?) {
        viewModelScope.launch {
            var elevation = 0.0
            var cityName = forcedName ?: "Memuat kota..."
            withContext(Dispatchers.IO) {
                if (forcedName == null) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(lat, lng, 1)
                        if (!addresses.isNullOrEmpty()) {
                            cityName = addresses[0].locality
                                ?: addresses[0].subAdminArea
                                ?: addresses[0].adminArea
                                ?: "Lokasi Tidak Diketahui"
                        } else {
                            cityName = "Lat: ${String.format(Locale.US, "%.2f", lat)}, Lng: ${String.format(Locale.US, "%.2f", lng)}"
                        }
                    } catch (e: Exception) {
                        cityName = "Lat: ${String.format(Locale.US, "%.2f", lat)}, Lng: ${String.format(Locale.US, "%.2f", lng)}"
                    }
                }

                try {
                    val url = URL("https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lng")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
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

            // Save to LocationPreferences
            val locationPrefs = LocationPreferences(context)
            locationPrefs.saveLocation(lat, lng, cityName, elevation)

            val prayerTimes = PrayerCalculator.calculatePrayerTimes(
                latitude = lat,
                longitude = lng,
                elevation = elevation
            )

            _uiState.update {
                it.copy(
                    prayerTimes = prayerTimes,
                    locationText = "$cityName (${elevation.toInt()} mdpl)",
                    isLoading = false
                )
            }

            // Schedule Alarms & Update Widget
            AlarmScheduler.scheduleAllPrayers(context, lat, lng, elevation)
            JadwalSholatWidget.updateAllWidgets(context)
        }
    }
}
