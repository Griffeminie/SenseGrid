package com.example.myapplication


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.database.DatabaseReference

@Composable
fun WiFiSettingsPage(
    navController: NavHostController,
    wifiRef: DatabaseReference // Firebase reference to /wifi
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("WiFi SSID") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("WiFi Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = { navController.popBackStack() }) {
                Text("Back")
            }

            Button(
                onClick = {
                    isSaving = true
                    // Update Firebase
                    wifiRef.child("ssid").setValue(ssid)
                    wifiRef.child("password").setValue(password)
                    isSaving = false
                }
            ) {
                Text(if (isSaving) "Saving..." else "Confirm")
            }
        }
    }
}
