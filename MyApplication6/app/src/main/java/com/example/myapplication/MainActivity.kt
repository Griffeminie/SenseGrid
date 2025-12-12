package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.database.*

data class Reading(
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val light: Int = 0,
    val timestamp: String = ""
)

class MainActivity : ComponentActivity() {

    private val database = FirebaseDatabase.getInstance()
    private val uploadEnabledRef = database.getReference("control/upload_enabled")
    private val readingsRef = database.getReference("readings")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(navController = navController,
                        uploadEnabledRef = uploadEnabledRef,
                        readingsRef = readingsRef
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: androidx.navigation.NavHostController,
    uploadEnabledRef: DatabaseReference,
    readingsRef: DatabaseReference
) {

    var isUploadEnabled by remember { mutableStateOf(false) }
    var latestReading by remember { mutableStateOf(Reading()) }

    // Firebase listener for upload toggle
    LaunchedEffect(Unit) {
        uploadEnabledRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isUploadEnabled = snapshot.getValue(Boolean::class.java) ?: false
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Firebase listener for new readings
    LaunchedEffect(Unit) {
        readingsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val reading = snapshot.getValue(Reading::class.java)
                if (reading != null) {
                    latestReading = reading.copy() // ensures Compose recomposes
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val reading = snapshot.getValue(Reading::class.java)
                if (reading != null) latestReading = reading.copy()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = "Temperature Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SenseGrid",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)         // ensures content avoids status bar + app bar
                .padding(16.dp),               // your extra spacing
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* ---------- CARDS AT THE TOP ---------- */

            TemperatureCard(latestReading.temperature)
            HumidityCard(latestReading.humidity)
            LightReading(latestReading.light)

            Text(
                text = "Last updated: ${latestReading.timestamp}",
                style = MaterialTheme.typography.bodyMedium
            )

            Divider()

            /* ---------- CONTROLS BELOW ---------- */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Turn On/Off", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isUploadEnabled,
                    onCheckedChange = {
                        isUploadEnabled = it
                        uploadEnabledRef.setValue(it)
                    }
                )
            }

            Button(
                onClick = { navController.navigate("info") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Go to Info Page")
            }
        }
    }
}

@Composable
fun HumidityCard(humidity: Float) {
    val valueText = "${humidity.toInt()} %"

    val description = when {
        humidity < 30f -> "Low"
        humidity in 30f..60f -> "Normal"
        else -> "High"
    }

    val targetContainerColor = lerp(
        Color(0xFF81D4FA),
        Color(0xFF0D47A1),
        (humidity.coerceIn(0f, 100f) / 100f)
    )
    val containerColor by animateColorAsState(targetValue = targetContainerColor)
    val targetIconColor = if (containerColor.luminance() < 0.5f) Color.White else Color.Black
    val iconColor by animateColorAsState(targetValue = targetIconColor)

    val criticalNote =
        if (humidity < 20f) "⚠ Warning: Humidity too low"
        else if (humidity > 80f) "⚠ Warning: High humidity!"
        else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InvertColors, contentDescription = "Humidity", tint = iconColor)
                    Spacer(Modifier.width(8.dp))
                    Text(description, color = iconColor)
                }
                Text(valueText, color = iconColor)
            }
        }

        criticalNote?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun TemperatureCard(temperature: Float) {
    val valueText = "${temperature.toInt()} °C"

    val description = when {
        temperature < 24f -> "Cool"
        temperature in 24f..27f -> "Comfortable"
        temperature in 28f..30f -> "Warm"
        temperature in 31f..33f -> "Hot"
        else -> "Very Hot"
    }

    val targetContainerColor = when {
        temperature < 24f -> Color.Cyan
        temperature in 24f..27f -> Color(0xFF4CAF50)
        temperature in 28f..30f -> Color.Yellow
        temperature in 31f..33f -> Color(0xFFFF9800)
        else -> Color.Red
    }

    val containerColor by animateColorAsState(targetValue = targetContainerColor)
    val targetIconColor = if (containerColor.luminance() < 0.5f) Color.White else Color.Black
    val iconColor by animateColorAsState(targetValue = targetIconColor)

    val criticalNote =
        if (temperature >= 40f) "⚠ Warning: Critical heat!"
        else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeviceThermostat, contentDescription = "Temp", tint = iconColor)
                    Spacer(Modifier.width(8.dp))
                    Text(description, color = iconColor)
                }
                Text(valueText, color = iconColor)
            }
        }

        criticalNote?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun LightReading(lightValue: Int) {
    val description = when (lightValue) {
        in 0..200 -> "Dark"
        in 201..400 -> "Dim"
        in 401..600 -> "Normal"
        in 601..800 -> "Bright"
        else -> "Very Bright"
    }

    val targetContainerColor =
        lerp(Color.DarkGray, Color.Yellow, (lightValue.coerceIn(0, 900) / 900f))
    val containerColor by animateColorAsState(targetValue = targetContainerColor)
    val targetIconColor = if (containerColor.luminance() < 0.5f) Color.White else Color.Black
    val iconColor by animateColorAsState(targetValue = targetIconColor)

    val criticalNote =
        if (lightValue < 20) "⚠ Warning: Light level critically low!"
        else null

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(80.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = "Light", tint = iconColor)
                    Spacer(Modifier.width(8.dp))
                    Text(description, color = iconColor)
                }
                Text("$lightValue", color = iconColor)
            }
        }

        criticalNote?.let {
            Text(it, color = Color.Red, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
