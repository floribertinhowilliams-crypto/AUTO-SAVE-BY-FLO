package com.floribert.autosaveflopro.ui.screens.lock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository
) : ViewModel() {
    fun checkPin(pin: String): Boolean = licenseRepository.getAppPin() == pin
    fun isLockEnabled(): Boolean = licenseRepository.isAppLockEnabled()
}

@Composable
fun LockScreen(onAuthenticated: () -> Unit, viewModel: LockViewModel = hiltViewModel()) {
    var pinInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("App Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Enter your PIN to continue", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 4) pinInput = it },
            label = { Text("Enter 4-digit PIN") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        if (error.isNotBlank()) {
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        
        Button(
            onClick = {
                if (viewModel.checkPin(pinInput)) {
                    onAuthenticated()
                } else {
                    error = "Invalid PIN. Please try again."
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Unlock App")
        }
    }
}
