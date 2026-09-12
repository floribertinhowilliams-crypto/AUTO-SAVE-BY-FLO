package com.floribert.autosaveflopro.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floribert.autosaveflopro.data.repository.ContactRepository
import com.floribert.autosaveflopro.notification.NotificationHelper
import com.floribert.autosaveflopro.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.floribert.autosaveflopro.ui.components.*

data class HomeUiState(
    val isNotificationAccessGranted: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val isAccessibilityEnabled: Boolean = false
)

@HiltViewModel
class HomeViewModel @javax.inject.Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ContactRepository,
    private val licenseRepository: com.floribert.autosaveflopro.data.repository.LicenseRepository
) : ViewModel() {
    val recentDetections = repository.getAllDetectedContacts().map { it.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val detectedCount = repository.getDetectedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    var isScanning by mutableStateOf(false)
    var scanResult by mutableStateOf<String?>(null)
    var isAutoScanEnabled by mutableStateOf(licenseRepository.isAutoScanEnabled())

    fun checkPermissionsState() = HomeUiState(
        NotificationHelper.isNotificationAccessGranted(context),
        PermissionUtils.hasContactPermissions(context),
        PermissionUtils.isAccessibilityServiceEnabled(context)
    )

    fun toggleAutoScan(enabled: Boolean) {
        licenseRepository.setAutoScanEnabled(enabled)
        isAutoScanEnabled = enabled
    }

    private fun isGuardianRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (com.floribert.autosaveflopro.notification.FloatingGuardianService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun toggleGuardian() {
        val running = isGuardianRunning()
        if (!running) {
            context.startService(Intent(context, com.floribert.autosaveflopro.notification.FloatingGuardianService::class.java))
        } else {
            context.stopService(Intent(context, com.floribert.autosaveflopro.notification.FloatingGuardianService::class.java))
        }
    }

    fun startDeepScan() {
        if (!licenseRepository.isLicenseActive()) {
            scanResult = "License Expired. Please subscribe to use Deep Scan."
            return
        }
        viewModelScope.launch {
            isScanning = true
            val res = repository.scanAndSaveUnnamedContacts()
            scanResult = when (res) {
                -1 -> "Permission Denied. Please grant 'Logs Access' in Manage Permissions."
                -2 -> "An error occurred during scan. Please try again."
                0 -> "Scan Complete: No new unsaved numbers found."
                else -> "Deep Scan Complete: $res new numbers found!"
            }
            isScanning = false
        }
    }
}

private fun shareApp(context: Context) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        val message = "Habari! Natumia AutoSave Contacts Pro (ACP) kukuza biashara yangu. Inanisaidia kusave namba za WhatsApp kiotomatiki na kutuma SMS za shukrani. \n\nIli kupata app hii, wasiliana nami nitumie faili la APK (ACP-release.apk) kisha utumie kodi yangu ya mualiko: ACP-762"
        putExtra(Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share AutoSave Pro")
    context.startActivity(shareIntent)
}

@Composable
fun HomeScreen(
    onNavigateToPermissions: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSavedContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToVideoDownloader: () -> Unit,
    onNavigateToBulkSms: () -> Unit,
    onNavigateToQr: () -> Unit,
    onNavigateToDirectChat: () -> Unit,
    onNavigateToBio: () -> Unit,
    onNavigateToGuide: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(HomeUiState()) }
    val recent by viewModel.recentDetections.collectAsState()
    val count by viewModel.detectedCount.collectAsState()
    LaunchedEffect(Unit) { state = viewModel.checkPermissionsState() }
    val active = state.isNotificationAccessGranted && state.hasContactsPermission

    Scaffold(
        topBar = { 
            AppHeader("AutoSave Contacts Pro")
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToGuide,
                icon = { Icon(Icons.AutoMirrored.Filled.HelpCenter, null) },
                text = { Text("User Guide") },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
    ) { p ->
        LazyColumn(Modifier.fillMaxSize().padding(p).padding(horizontal = 16.dp)) {
            item { Spacer(Modifier.height(12.dp)); StatusCard(active); Spacer(Modifier.height(16.dp)) }

            // MASTER CONTROLS
            item {
                Text("Automation Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    // Manual Scan Button
                    Card(
                        onClick = { viewModel.startDeepScan() },
                        modifier = Modifier.weight(1f).height(110.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary),
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("SCAN MANUAL", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("Check All Logs", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                        }
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    // Auto Scan Button (Status/Toggle)
                    Card(
                        onClick = { 
                            if (!state.isAccessibilityEnabled) onNavigateToPermissions()
                            else viewModel.toggleAutoScan(!viewModel.isAutoScanEnabled) 
                        },
                        modifier = Modifier.weight(1f).height(110.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.isAutoScanEnabled && state.isAccessibilityEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                        ),
                        elevation = CardDefaults.cardElevation(6.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(if (viewModel.isAutoScanEnabled && state.isAccessibilityEnabled) Icons.Default.AutoAwesome else Icons.Default.GppBad, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("SCAN AUTO", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text(
                                if (!state.isAccessibilityEnabled) "Needs Permission"
                                else if (viewModel.isAutoScanEnabled) "ON (Running)" 
                                else "OFF (Disabled)", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = Color.White.copy(0.7f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            item {
                Card(
                    Modifier.fillMaxWidth().height(100.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Database Growth", fontWeight = FontWeight.Bold)
                            Text("Your contacts are increasing!", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { (count / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                            )
                        }
                        Text("$count", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (viewModel.isScanning) {
                item {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    Text("Scanning phone logs for unsaved numbers...", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            viewModel.scanResult?.let { res ->
                item {
                    Text(res, color = if (res.contains("Expired")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Button(onClick = { viewModel.scanResult = null }) { Text("Dismiss") }
                }
            }

            item {
                Row(Modifier.fillMaxWidth()) {
                    StatCard("Detected", count, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp)); StatCard("Saved", 0, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp)); StatCard("Today", 0, Modifier.weight(1f))
                }
            }
            item {
                SectionHeader("WhatsApp Magic Tools")
                Row(Modifier.fillMaxWidth()) {
                    QuickActionCard("Direct Chat", Icons.AutoMirrored.Filled.Chat, onNavigateToDirectChat, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    QuickActionCard("Smart Bio", Icons.Default.ContactPage, onNavigateToBio, Modifier.weight(1f))
                }
                SectionHeader("Utilities & Downloader")
                Row(Modifier.fillMaxWidth()) {
                    QuickActionCard("Status Saver", Icons.Default.PhotoLibrary, onNavigateToStatus, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    QuickActionCard("Video Downloader", Icons.Default.CloudDownload, onNavigateToVideoDownloader, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    QuickActionCard("Bulk SMS", Icons.AutoMirrored.Filled.Send, onNavigateToBulkSms, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    QuickActionCard("QR Tool", Icons.Default.QrCode, onNavigateToQr, Modifier.weight(1f))
                }
                SectionHeader("Subscription & Status")
                QuickActionCard("Account & Subscription", Icons.Default.CardMembership, onNavigateToSubscription)
                SectionHeader("Quick Actions")
                QuickActionCard("Manage Permissions", Icons.Default.Security, onNavigateToPermissions)
                QuickActionCard("Share with Friends", Icons.Default.Share, { shareApp(context) })
                QuickActionCard("Detection History", Icons.Default.History, onNavigateToHistory)
                QuickActionCard("Saved Contacts", Icons.Default.PersonAdd, onNavigateToSavedContacts)
                QuickActionCard("Settings", Icons.Default.Settings, onNavigateToSettings)
                SectionHeader("Recent Detections")
            }
            if (recent.isEmpty()) item {
                EmptyState("No contacts detected yet.", subtitle = "Phase 03 will add notification parsing.")
            } else items(recent) { ContactCard(it) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
