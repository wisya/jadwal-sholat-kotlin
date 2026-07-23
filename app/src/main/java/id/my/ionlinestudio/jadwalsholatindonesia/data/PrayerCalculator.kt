package id.my.ionlinestudio.jadwalsholatindonesia.data

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.Qibla
import com.batoulapps.adhan.data.DateComponents
import java.util.Date
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PrayerCalculator {

    fun getParameters(elevation: Double = 0.0): CalculationParameters {
        val params = CalculationMethod.OTHER.parameters
        params.fajrAngle = 20.0
        params.ishaAngle = 18.0
        params.madhab = Madhab.SHAFI

        var elevationCorrection = 0
        if (elevation > 0) {
            val dipAngle = 0.0347 * sqrt(elevation)
            elevationCorrection = (dipAngle * 4).roundToInt()
        }

        params.adjustments.fajr = 2
        params.adjustments.sunrise = -2 - elevationCorrection
        params.adjustments.dhuhr = 2
        params.adjustments.asr = 2
        params.adjustments.maghrib = 2 + elevationCorrection
        params.adjustments.isha = 2

        return params
    }

    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        elevation: Double = 0.0,
        date: Date = Date()
    ): PrayerTimes {
        val coordinates = Coordinates(latitude, longitude)
        val params = getParameters(elevation)
        val dateComponents = DateComponents.from(date)
        return PrayerTimes(coordinates, dateComponents, params)
    }

    fun calculateQiblaDirection(latitude: Double, longitude: Double): Double {
        val coordinates = Coordinates(latitude, longitude)
        return Qibla(coordinates).direction
    }
}
