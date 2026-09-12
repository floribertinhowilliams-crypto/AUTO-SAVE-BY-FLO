package com.floribert.autosaveflopro.ui.screens.magic

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floribert.autosaveflopro.ui.components.AppHeader

@Composable
fun WhatsAppDirectScreen(onBackClicked: () -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(topBar = { AppHeader("WhatsApp Magic Chat") }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Message Without Saving", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Enter a WhatsApp number to chat directly.", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(32.dp))
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (with Country Code)") },
                placeholder = { Text("e.g. 255762983844") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (phoneNumber.isNotBlank()) {
                        openWhatsApp(context, phoneNumber, message)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Open in WhatsApp")
            }
            
            Spacer(Modifier.weight(1f))
            Button(onClick = onBackClicked, Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors()) {
                Text("Back to Dashboard")
            }
        }
    }
}

private fun openWhatsApp(context: Context, number: String, msg: String) {
    val cleanNumber = number.replace("+", "").replace(" ", "")
    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(msg)}")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}
