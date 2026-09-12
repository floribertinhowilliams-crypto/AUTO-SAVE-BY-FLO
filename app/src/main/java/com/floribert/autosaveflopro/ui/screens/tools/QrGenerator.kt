package com.floribert.autosaveflopro.ui.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floribert.autosaveflopro.ui.components.AppHeader

@Composable
fun QrGeneratorScreen(onBackClicked: () -> Unit) {
    var textInput by remember { mutableStateOf("") }

    Scaffold(topBar = { AppHeader("QR Generator") }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Custom QR Codes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Generate QR for your WhatsApp link, website, or WiFi.", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Enter Text or URL") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(32.dp))
            
            if (textInput.isNotBlank()) {
                Card(Modifier.size(250.dp)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode, null, Modifier.size(200.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { /* Save QR */ }) {
                    Text("Save QR to Gallery")
                }
            } else {
                EmptyStateQr()
            }
            
            Spacer(Modifier.weight(1f))
            Button(onClick = onBackClicked, Modifier.fillMaxWidth()) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
fun EmptyStateQr() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.QrCode, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Text("Your QR Code will appear here", color = MaterialTheme.colorScheme.outline)
    }
}
