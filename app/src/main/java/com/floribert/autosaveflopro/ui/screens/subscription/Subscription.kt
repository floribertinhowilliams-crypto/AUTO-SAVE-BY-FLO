package com.floribert.autosaveflopro.ui.screens.subscription

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import com.floribert.autosaveflopro.ui.components.AppHeader
import com.floribert.autosaveflopro.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository
) : ViewModel() {
    var expiryDate by mutableStateOf(licenseRepository.getExpiryTimestamp())
    var remainingDays by mutableStateOf(licenseRepository.getRemainingDays())
    var isTrialAvailable by mutableStateOf(licenseRepository.isTrialAvailable())

    fun refresh() {
        expiryDate = licenseRepository.getExpiryTimestamp()
        remainingDays = licenseRepository.getRemainingDays()
        isTrialAvailable = licenseRepository.isTrialAvailable()
    }

    fun useTrial() {
        licenseRepository.useTrial()
        refresh()
    }

    fun activateCode(code: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = licenseRepository.validateAndUseVoucher(code)
            if (result == "SUCCESS") refresh()
            onResult(result)
        }
    }
}

@Composable
fun SubscriptionScreen(onBackClicked: () -> Unit, viewModel: SubscriptionViewModel = hiltViewModel()) {
    var codeInput by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(topBar = { AppHeader("Subscription Plans") }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("License Status", fontWeight = FontWeight.Bold)
                    if (viewModel.remainingDays > 0) {
                        Text("Active: ${viewModel.remainingDays} days remaining", style = MaterialTheme.typography.headlineSmall)
                        Text("Expires on: ${dateFormat.format(Date(viewModel.expiryDate))}")
                    } else {
                        Text("Expired / No Active License", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Subscription Plans", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            PlanCard("Weekly Plan", "Tsh ${Constants.PLAN_WEEKLY_PRICE}", Icons.Default.CardMembership)
            PlanCard("Monthly Plan", "Tsh ${Constants.PLAN_MONTHLY_PRICE}", Icons.Default.CardMembership)
            PlanCard("3 Months Plan", "Tsh ${Constants.PLAN_3MONTHS_PRICE}", Icons.Default.CardMembership)

            if (viewModel.isTrialAvailable) {
                Button(onClick = { viewModel.useTrial() }, Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text("Start 1 Day Free Trial")
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payment, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Payment Details", fontWeight = FontWeight.Bold)
                    }
                    Text("Send payment to:")
                    Text("M-Pesa: ${Constants.PAYMENT_NUMBER}", fontWeight = FontWeight.Bold)
                    Text("Name: ${Constants.PAYMENT_NAME}")
                    Text("After payment, contact admin for your activation code.")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Activate License", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = codeInput,
                onValueChange = { codeInput = it },
                label = { Text("Enter Voucher Code") },
                modifier = Modifier.fillMaxWidth()
            )
            if (message.isNotBlank()) {
                Text(message, color = if (message.contains("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    viewModel.activateCode(codeInput) { result ->
                        when (result) {
                            "SUCCESS" -> {
                                message = "Success! License Activated."
                                codeInput = ""
                            }
                            "USED" -> {
                                message = "Kosa: Vocha hii tayari imeshatumika!"
                            }
                            else -> {
                                message = "Kosa: Vocha hii siyo halali!"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text("Activate Now")
            }
            
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBackClicked, Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors()) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
fun PlanCard(title: String, price: String, icon: ImageVector) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(price, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
