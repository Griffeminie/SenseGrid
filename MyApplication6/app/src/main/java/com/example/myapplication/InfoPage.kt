package com.example.myapplication

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.nativeCanvas
import androidx.navigation.NavHostController
import com.google.firebase.database.DatabaseReference
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun InfoPage(navController: NavHostController, readingsRef: DatabaseReference) {

    var readings by remember { mutableStateOf(listOf<Reading>()) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Legend toggles
    var showTemperature by remember { mutableStateOf(true) }
    var showHumidity by remember { mutableStateOf(true) }
    var showLight by remember { mutableStateOf(true) }


    fun refreshData() {
        isRefreshing = true
        readingsRef.orderByKey().limitToLast(50).get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.children.mapNotNull { it.getValue(Reading::class.java) }
                readings = list
                isRefreshing = false
            }
            .addOnFailureListener { isRefreshing = false }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    fun formatTimestamp(ts: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = parser.parse(ts)
            val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
            formatter.format(date)
        } catch (e: Exception) {
            ts.take(5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Spacer(Modifier.weight(1f))

            // Centered title with icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeviceThermostat, contentDescription = "SenseGrid Icon", tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("SenseGrid", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(Modifier.weight(1f))

            // Refresh button
            Button(onClick = { refreshData() }) {
                Text(if (isRefreshing) "Refreshing..." else "Refresh")
            }
        }


        Spacer(Modifier.height(16.dp))

        // Legends stacked vertically
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showTemperature, onCheckedChange = { showTemperature = it })
                Icon(Icons.Default.DeviceThermostat, contentDescription = "Temp", tint = Color.Red)
                Spacer(Modifier.width(4.dp))
                Text("Temperature")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showHumidity, onCheckedChange = { showHumidity = it })
                Icon(Icons.Default.InvertColors, contentDescription = "Humidity", tint = Color.Blue)
                Spacer(Modifier.width(4.dp))
                Text("Humidity")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = showLight, onCheckedChange = { showLight = it })
                Icon(Icons.Default.WbSunny, contentDescription = "Light", tint = Color.Yellow)
                Spacer(Modifier.width(4.dp))
                Text("Light")
            }
        }

        Spacer(Modifier.height(16.dp))

// Graph Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFC1C1C1))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(40.dp)) {

                val width = size.width
                val height = size.height

                // Clamp readings to realistic ranges
                val tempValues = readings.map { it.temperature.coerceIn(-50f, 80f) }
                val humValues = readings.map { it.humidity.coerceIn(0f, 100f) }
                val lightValues = readings.map { it.light.toFloat().coerceIn(0f, 1023f) } // adjust to your LDR range

                val minTemp = tempValues.minOrNull() ?: 0f
                val maxTemp = tempValues.maxOrNull() ?: 1f
                val minHum = humValues.minOrNull() ?: 0f
                val maxHum = humValues.maxOrNull() ?: 1f
                val minLight = lightValues.minOrNull() ?: 0f
                val maxLight = lightValues.maxOrNull() ?: 1f

                fun mapY(value: Float, min: Float, max: Float) =
                    height - if (max - min == 0f) height / 2 else (value - min) / (max - min) * height

                val xStep = if (readings.size > 1) width / (readings.size - 1) else 1f

                for (i in 0 until readings.size - 1) {
                    val r1 = readings[i]
                    val r2 = readings[i + 1]

                    if (showTemperature) drawLine(
                        color = Color.Red,
                        start = Offset(i * xStep, mapY(r1.temperature.coerceIn(-50f, 80f), minTemp, maxTemp)),
                        end = Offset((i + 1) * xStep, mapY(r2.temperature.coerceIn(-50f, 80f), minTemp, maxTemp)),
                        strokeWidth = 4f
                    )
                    if (showHumidity) drawLine(
                        color = Color.Blue,
                        start = Offset(i * xStep, mapY(r1.humidity.coerceIn(0f, 100f), minHum, maxHum)),
                        end = Offset((i + 1) * xStep, mapY(r2.humidity.coerceIn(0f, 100f), minHum, maxHum)),
                        strokeWidth = 4f
                    )
                    if (showLight) drawLine(
                        color = Color.Yellow,
                        start = Offset(i * xStep, mapY(r1.light.toFloat().coerceIn(0f, 1023f), minLight, maxLight)),
                        end = Offset((i + 1) * xStep, mapY(r2.light.toFloat().coerceIn(0f, 1023f), minLight, maxLight)),
                        strokeWidth = 4f
                    )
                }

                // Y-axis labels (generic 0–100 scale for display)
                for (i in 0..5) {
                    val y = height - i * height / 5
                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            "${i * 20}",
                            0f,
                            y,
                            android.graphics.Paint().apply { textSize = 30f; color = android.graphics.Color.BLACK }
                        )
                    }
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                readings.forEachIndexed { index, r ->
                    if (index % 5 == 0 || index == readings.lastIndex) {
                        Text(formatTimestamp(r.timestamp), style = MaterialTheme.typography.labelSmall)
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }
            }
        }


        Spacer(Modifier.height(16.dp))

        // Graph Label showing which sensors are visible
        val visibleSensors = mutableListOf<String>()
        if (showTemperature) visibleSensors.add("Temperature")
        if (showHumidity) visibleSensors.add("Humidity")
        if (showLight) visibleSensors.add("Light")
        Text("${visibleSensors.joinToString(" & ")} Data", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))
    }
}
