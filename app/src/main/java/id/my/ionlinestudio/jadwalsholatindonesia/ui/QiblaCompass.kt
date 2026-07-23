package id.my.ionlinestudio.jadwalsholatindonesia.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.my.ionlinestudio.jadwalsholatindonesia.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun rememberCompassAzimuth(isLive: Boolean): Float {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(isLive) {
        if (!isLive) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    azimuth = azimuthDeg
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    var azimuthDeg = event.values[0]
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    azimuth = azimuthDeg
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return azimuth
}

fun getQiblaDirectionDescription(angle: Double): String {
    val deg = (angle * 10).roundToInt() / 10.0
    return when {
        deg >= 337.5 || deg < 22.5 -> "Utara ($deg°)"
        deg >= 22.5 && deg < 67.5 -> "Timur Laut — $deg° dari Utara ke Timur"
        deg >= 67.5 && deg < 112.5 -> "Timur — ${((90.0 - deg) * 10).roundToInt() / 10.0}° dari Timur"
        deg >= 112.5 && deg < 157.5 -> "Tenggara — ${((deg - 90.0) * 10).roundToInt() / 10.0}° dari Timur ke Selatan"
        deg >= 157.5 && deg < 202.5 -> "Selatan — ${((180.0 - deg) * 10).roundToInt() / 10.0}° dari Selatan"
        deg >= 202.5 && deg < 247.5 -> "Barat Daya — ${((deg - 180.0) * 10).roundToInt() / 10.0}° dari Selatan ke Barat"
        deg >= 247.5 && deg < 292.5 -> "Barat — ${((270.0 - deg) * 10).roundToInt() / 10.0}° dari Barat"
        deg >= 292.5 && deg < 337.5 -> "Barat Laut — ${((deg - 270.0) * 10).roundToInt() / 10.0}° dari Barat ke Utara"
        else -> "$deg°"
    }
}

@Composable
fun QiblaDialogContent(
    cityName: String,
    qiblaDegree: Double,
    onDismiss: () -> Unit
) {
    var isLiveCompass by remember { mutableStateOf(false) }
    val rawAzimuth = rememberCompassAzimuth(isLive = isLiveCompass)

    val animatedAzimuth by animateFloatAsState(
        targetValue = if (isLiveCompass) -rawAzimuth else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "CompassRotation"
    )

    val roundedDegree = (qiblaDegree * 10).roundToInt() / 10.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WebSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = WebPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Arah Kiblat",
                        color = WebTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = WebTextSecondary)
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${roundedDegree}°",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = WebPrimary
                )
                Text(
                    text = getQiblaDirectionDescription(qiblaDegree),
                    fontSize = 13.sp,
                    color = WebTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Compass Wrapper (220dp x 220dp)
                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Static Crosshairs
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 1.dp.toPx()
                        val color = WebOutline.copy(alpha = 0.3f)
                        // Vertical crosshair
                        drawLine(
                            color = color,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = strokeWidth
                        )
                        // Horizontal crosshair
                        drawLine(
                            color = color,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Rotating Compass Dial
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(animatedAzimuth)
                            .clip(CircleShape)
                            .background(WebSurfaceVariant)
                            .border(5.dp, WebPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Cardinal Marks
                        Box(modifier = Modifier.fillMaxSize()) {
                            // North (U) - Top
                            Text(
                                text = "U",
                                color = Color(0xFFEF5350),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                            )
                            // East (T) - Right
                            Text(
                                text = "T",
                                color = WebTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 10.dp)
                            )
                            // South (S) - Bottom
                            Text(
                                text = "S",
                                color = WebTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 8.dp)
                            )
                            // West (B) - Left
                            Text(
                                text = "B",
                                color = WebTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 10.dp)
                            )
                        }

                        // Qibla Pointer Needle & Kaaba Icon
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(qiblaDegree.toFloat()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Needle Line pointing to Qibla
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp)
                            ) {
                                drawLine(
                                    color = WebSecondary,
                                    start = Offset(size.width / 2, size.height / 2),
                                    end = Offset(size.width / 2, 0f),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }

                            // Kaaba Box Icon on top of Qibla Needle
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .size(30.dp)
                                    .rotate(-qiblaDegree.toFloat()) // Keep Kaaba box upright!
                                    .background(Color.Black, RoundedCornerShape(4.dp))
                                    .border(1.5.dp, WebGold, RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                // Kaaba Gold Band
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .padding(top = 2.dp)
                                        .background(WebGold)
                                )
                            }
                        }

                        // Center Pivot Point
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(WebPrimary, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sensor Live Toggle Switch
                Surface(
                    color = WebSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gunakan Sensor Kompas HP (Live)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = WebTextPrimary
                        )
                        Switch(
                            checked = isLiveCompass,
                            onCheckedChange = { isLiveCompass = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WebPrimary,
                                checkedTrackColor = WebPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Instruction Hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WebSurfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WebTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLiveCompass) "Pegang HP mendatar (sejajar lantai). Putar badan hingga ikon Ka'bah menunjuk ke atas." else "Mode Statis: Utara (0°/Atas), Timur (90°/Kanan), Selatan (180°/Bawah), Barat (270°/Kiri).",
                        fontSize = 11.sp,
                        color = WebTextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {}
    )
}
