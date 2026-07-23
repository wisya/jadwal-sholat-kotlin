package id.my.ionlinestudio.jadwalsholatindonesia.data

import android.content.Context
import android.content.SharedPreferences

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val elevation: Double
)

class LocationPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveLocation(latitude: Double, longitude: Double, cityName: String, elevation: Double) {
        prefs.edit().apply {
            putFloat(KEY_LAT, latitude.toFloat())
            putFloat(KEY_LNG, longitude.toFloat())
            putString(KEY_CITY, cityName)
            putFloat(KEY_ELEVATION, elevation.toFloat())
            apply()
        }
    }

    fun getLocation(): UserLocation {
        val lat = prefs.getFloat(KEY_LAT, DEFAULT_LAT.toFloat()).toDouble()
        val lng = prefs.getFloat(KEY_LNG, DEFAULT_LNG.toFloat()).toDouble()
        val city = prefs.getString(KEY_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
        val elevation = prefs.getFloat(KEY_ELEVATION, 0f).toDouble()
        return UserLocation(lat, lng, city, elevation)
    }

    companion object {
        private const val PREFS_NAME = "jadwal_sholat_prefs"
        private const val KEY_LAT = "latitude"
        private const val KEY_LNG = "longitude"
        private const val KEY_CITY = "city_name"
        private const val KEY_ELEVATION = "elevation"

        const val DEFAULT_LAT = -7.8014
        const val DEFAULT_LNG = 110.3644
        const val DEFAULT_CITY = "Yogyakarta"
    }
}
