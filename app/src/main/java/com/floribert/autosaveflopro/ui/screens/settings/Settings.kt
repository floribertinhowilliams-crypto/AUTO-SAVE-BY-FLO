package com.floribert.autosaveflopro.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.floribert.autosaveflopro.notification.NotificationHelper
import com.floribert.autosaveflopro.notification.FloatingGuardianService
import com.floribert.autosaveflopro.utils.PermissionUtils
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import com.floribert.autosaveflopro.ui.components.*
import javax.inject.Inject

data class SettingsState(
    val isNotificationAccessGranted: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val isSmsEnabled: Boolean = false,
    val smsMessage: String = "",
    val isAppLockEnabled: Boolean = false,
    val appPin: String = "",
    val themeMode: Int = 0,
    val isInAppNotifEnabled: Boolean = true,
    val isWaReplyEnabled: Boolean = false,
    val waReplyMsg: String = "",
    val contactTemplate: String = "",
    val isGuardianEnabled: Boolean = false,
    val isAutoScanEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val licenseRepository: LicenseRepository
) : ViewModel() {
    fun getSettingsState() = SettingsState(
        NotificationHelper.isNotificationAccessGranted(context),
        PermissionUtils.hasContactPermissions(context),
        licenseRepository.isSmsEnabled(),
        licenseRepository.getSmsMessage(),
        licenseRepository.isAppLockEnabled(),
        licenseRepository.getAppPin(),
        licenseRepository.getThemeMode(),
        licenseRepository.isInAppNotifEnabled(),
        licenseRepository.isWaReplyEnabled(),
        licenseRepository.getWaReplyMsg(),
        licenseRepository.getContactTemplate(),
        isGuardianRunning(context),
        licenseRepository.isAutoScanEnabled()
    )

    private fun isGuardianRunning(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (FloatingGuardianService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun toggleGuardian(enabled: Boolean) {
        if (enabled) {
            context.startService(Intent(context, FloatingGuardianService::class.java))
        } else {
            context.stopService(Intent(context, FloatingGuardianService::class.java))
        }
    }

    fun toggleAutoScan(enabled: Boolean) {
        licenseRepository.setAutoScanEnabled(enabled)
    }

    fun toggleSms(enabled: Boolean) {
        licenseRepository.setSmsEnabled(enabled)
    }

    fun updateSmsMessage(message: String) {
        licenseRepository.setSmsMessage(message)
    }

    fun toggleAppLock(enabled: Boolean) {
        licenseRepository.setAppLockEnabled(enabled)
    }

    fun updateAppPin(pin: String) {
        licenseRepository.setAppPin(pin)
    }

    fun setThemeMode(mode: Int) {
        licenseRepository.setThemeMode(mode)
    }

    fun toggleInAppNotif(enabled: Boolean) {
        licenseRepository.setInAppNotifEnabled(enabled)
    }

    fun toggleWaReply(enabled: Boolean) {
        licenseRepository.setWaReplyEnabled(enabled)
    }

    fun updateWaReplyMsg(msg: String) {
        licenseRepository.setWaReplyMsg(msg)
    }

    fun updateContactTemplate(template: String) {
        licenseRepository.setContactTemplate(template)
    }
}

@Composable
fun SettingsScreen(onBackClicked: () -> Unit, onNavigateToPermissions: () -> Unit, onNavigateToAdmin: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    var state by remember { mutableStateOf(SettingsState()) }
    var smsMessageInput by remember { mutableStateOf("") }
    var appPinInput by remember { mutableStateOf("") }
    var waReplyInput by remember { mutableStateOf("") }
    var templateInput by remember { mutableStateOf("") }
    
    var adminTapCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { 
        state = viewModel.getSettingsState()
        smsMessageInput = state.smsMessage
        appPinInput = state.appPin
        waReplyInput = state.waReplyMsg
        templateInput = state.contactTemplate
    }

    Scaffold(topBar = { AppHeader("Settings") }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            SectionHeader("AUTOMATION")
            SettingRow("AutoSave Status", value = if (state.isNotificationAccessGranted && state.hasContactsPermission) "Active" else "Inactive", onClick = onNavigateToPermissions)
            
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Floating Guardian", fontWeight = FontWeight.Medium)
                    Text("Persistent bubble for real-time status", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isGuardianEnabled, onCheckedChange = { 
                    viewModel.toggleGuardian(it)
                    state = state.copy(isGuardianEnabled = it)
                })
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic Screen Scan", fontWeight = FontWeight.Medium)
                    Text("Detect numbers while scrolling apps", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isAutoScanEnabled, onCheckedChange = { 
                    viewModel.toggleAutoScan(it)
                    state = state.copy(isAutoScanEnabled = it)
                })
            }

            OutlinedTextField(
                value = templateInput,
                onValueChange = { 
                    templateInput = it
                    viewModel.updateContactTemplate(it)
                },
                label = { Text("Default Contact Name (if no name found)") },
                placeholder = { Text("e.g. January, Customer, Leads") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true
            )
            Text("Example: ${templateInput} 1234", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

            SectionHeader("SECURITY & PRIVACY")
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("App Lock (PIN)", fontWeight = FontWeight.Medium)
                    Text("Request PIN when opening app", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isAppLockEnabled, onCheckedChange = { 
                    viewModel.toggleAppLock(it)
                    state = state.copy(isAppLockEnabled = it)
                })
            }
            
            if (state.isAppLockEnabled) {
                OutlinedTextField(
                    value = appPinInput,
                    onValueChange = { 
                        if (it.length <= 4) {
                            appPinInput = it
                            viewModel.updateAppPin(it)
                        }
                    },
                    label = { Text("Set 4-digit PIN") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    singleLine = true
                )
            }

            SectionHeader("SMS AUTO-REPLY")
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Send Thank You SMS", fontWeight = FontWeight.Medium)
                    Text("Auto-send SMS after saving contact", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isSmsEnabled, onCheckedChange = { 
                    viewModel.toggleSms(it)
                    state = state.copy(isSmsEnabled = it)
                })
            }
            
            if (state.isSmsEnabled) {
                OutlinedTextField(
                    value = smsMessageInput,
                    onValueChange = { 
                        smsMessageInput = it
                        viewModel.updateSmsMessage(it)
                    },
                    label = { Text("Custom SMS Message") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    minLines = 2
                )
            }

            SectionHeader("WHATSAPP AUTO-REPLY (BOT)")
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Enable WhatsApp Bot", fontWeight = FontWeight.Medium)
                    Text("Auto-reply directly on WhatsApp", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isWaReplyEnabled, onCheckedChange = { 
                    viewModel.toggleWaReply(it)
                    state = state.copy(isWaReplyEnabled = it)
                })
            }
            
            if (state.isWaReplyEnabled) {
                OutlinedTextField(
                    value = waReplyInput,
                    onValueChange = { 
                        waReplyInput = it
                        viewModel.updateWaReplyMsg(it)
                    },
                    label = { Text("WhatsApp Reply Message") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    minLines = 2
                )
            }

            SectionHeader("PREFERENCES")
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Theme Mode", fontWeight = FontWeight.Medium)
                    Text(when(state.themeMode) { 0 -> "Follow System"; 1 -> "Light Mode"; else -> "Dark Mode" }, style = MaterialTheme.typography.bodySmall)
                }
                var showDialog by remember { mutableStateOf(false) }
                Button(onClick = { showDialog = true }) { Text("Change") }
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Select Theme") },
                        text = {
                            Column {
                                listOf("System", "Light", "Dark").forEachIndexed { index, name ->
                                    Row(Modifier.fillMaxWidth().clickable { 
                                        viewModel.setThemeMode(index)
                                        state = state.copy(themeMode = index)
                                        showDialog = false
                                    }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = state.themeMode == index, onClick = null)
                                        Text(name, Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("In-App Notifications", fontWeight = FontWeight.Medium)
                    Text("Alert when a new contact is saved", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = state.isInAppNotifEnabled, onCheckedChange = { 
                    viewModel.toggleInAppNotif(it)
                    state = state.copy(isInAppNotifEnabled = it)
                })
            }
            SectionHeader("PRIVACY & ABOUT")
            SettingRow("On-Device Engine", "100% local processing. No external cloud or AI APIs used.")
            
            // Hidden Admin Access: Tap 5 times on App Version
            SettingRow(
                title = "App Version", 
                subtitle = "1.0.0 (Phase 04 Premium)", 
                onClick = {
                    adminTapCount++
                    if (adminTapCount >= 5) {
                        onNavigateToAdmin()
                        adminTapCount = 0
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
