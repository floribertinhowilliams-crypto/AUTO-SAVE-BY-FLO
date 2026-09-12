package com.floribert.autosaveflopro.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import com.floribert.autosaveflopro.ui.components.AppHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository
) : ViewModel() {
    var generatedCode by mutableStateOf("")

    fun generate(days: Int) {
        viewModelScope.launch {
            generatedCode = licenseRepository.generateVoucher(days)
        }
    }

    fun checkAdminPin(pin: String): Boolean = licenseRepository.getAdminPin() == pin
    fun checkTokenPin(pin: String): Boolean = licenseRepository.getTokenGenPin() == pin
    fun updateTokenPin(newPin: String) {
        viewModelScope.launch {
            licenseRepository.setTokenGenPin(newPin)
        }
    }
}

@Composable
fun AdminDashboard(onBackClicked: () -> Unit, viewModel: AdminViewModel = hiltViewModel()) {
    var loginPin by remember { mutableStateOf("") }
    var tokenPin by remember { mutableStateOf("") }
    
    var isAdminAuthenticated by remember { mutableStateOf(false) }
    var isTokenAuthenticated by remember { mutableStateOf(false) }
    
    var error by remember { mutableStateOf("") }

    if (!isAdminAuthenticated) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.AdminPanelSettings, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Admin Login", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Enter Primary Admin PIN", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = loginPin,
                onValueChange = { 
                    loginPin = it 
                    error = ""
                },
                label = { Text("Admin PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            
            Button(
                onClick = { 
                    if (viewModel.checkAdminPin(loginPin)) {
                        isAdminAuthenticated = true 
                        error = ""
                    } else {
                        error = "Incorrect Admin PIN."
                    }
                }, 
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
            ) {
                Text("Login to Dashboard")
            }
            
            TextButton(onClick = onBackClicked, modifier = Modifier.padding(top = 16.dp)) {
                Text("Cancel")
            }
        }
    } else if (!isTokenAuthenticated) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.VpnKey, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
            Text("Token Security", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Enter Token Generation Password", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = tokenPin,
                onValueChange = { 
                    tokenPin = it 
                    error = ""
                },
                label = { Text("Token Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            
            Button(
                onClick = { 
                    if (viewModel.checkTokenPin(tokenPin)) {
                        isTokenAuthenticated = true 
                        error = ""
                    } else {
                        error = "Incorrect Token Password."
                    }
                }, 
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Unlock Token Generator")
            }
            
            TextButton(onClick = { isAdminAuthenticated = false }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Logout Admin")
            }
        }
    } else {
        Scaffold(topBar = { AppHeader("Secure Voucher Admin") }) { p ->
            Column(Modifier.fillMaxSize().padding(p).padding(16.dp)) {
                Text("Secure Voucher Generator", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("100% Secured Alphanumeric Codes", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                
                Button(onClick = { viewModel.generate(7) }, Modifier.fillMaxWidth()) { Text("Generate 1 Week Code") }
                Button(onClick = { viewModel.generate(30) }, Modifier.fillMaxWidth()) { Text("Generate 1 Month Code") }
                Button(onClick = { viewModel.generate(365) }, Modifier.fillMaxWidth()) { Text("Generate 1 Year Code") }
                
                if (viewModel.generatedCode.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("New Secure Voucher:", fontWeight = FontWeight.Bold)
                            Text(viewModel.generatedCode, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Voucher Code", viewModel.generatedCode)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Token imekopiwa!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Kopi Token (Copy)")
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Copy and send to client. This code is unique and expires after one use.")
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                Text("System Control", fontWeight = FontWeight.Bold)
                Text("Status: Online & Secured")
                
                Spacer(Modifier.height(16.dp))
                var showChangePinDialog by remember { mutableStateOf(false) }
                var newPinText by remember { mutableStateOf("") }
                
                Button(
                    onClick = { showChangePinDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Change Token Password")
                }

                if (showChangePinDialog) {
                    AlertDialog(
                        onDismissRequest = { showChangePinDialog = false },
                        title = { Text("Change Token Password") },
                        text = {
                            Column {
                                Text("Enter your new secure token generation password:")
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = newPinText,
                                    onValueChange = { newPinText = it },
                                    label = { Text("New Password") },
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newPinText.isNotBlank()) {
                                        viewModel.updateTokenPin(newPinText)
                                        showChangePinDialog = false
                                        newPinText = ""
                                    }
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showChangePinDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { isTokenAuthenticated = false }, Modifier.weight(1f)) {
                        Text("Lock Tokens")
                    }
                    Button(onClick = onBackClicked, Modifier.weight(1f)) {
                        Text("Exit Dashboard")
                    }
                }
            }
        }
    }
}
