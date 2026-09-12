package com.floribert.autosaveflopro.ui.screens.permission

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.floribert.autosaveflopro.notification.NotificationHelper
import com.floribert.autosaveflopro.utils.PermissionUtils
import com.floribert.autosaveflopro.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.content.Intent
import android.os.Build
import javax.inject.Inject

data class PermissionState(
    val isNotificationAccessGranted: Boolean = false,
    val hasContactsPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasLogPermissions: Boolean = false,
    val hasNotifPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val hasStoragePermission: Boolean = false
)

@HiltViewModel
class PermissionViewModel @Inject constructor(@ApplicationContext private val context: Context) : ViewModel() {
    fun getPermissionState() = PermissionState(
        NotificationHelper.isNotificationAccessGranted(context),
        PermissionUtils.hasContactPermissions(context),
        PermissionUtils.hasSmsPermission(context),
        PermissionUtils.hasLogPermissions(context),
        PermissionUtils.hasNotificationPermission(context),
        PermissionUtils.hasOverlayPermission(context),
        PermissionUtils.isAccessibilityServiceEnabled(context),
        PermissionUtils.hasStoragePermission(context)
    )
    fun openNotificationAccessSettings() = NotificationHelper.openNotificationAccessSettings(context)
    
    fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

@Composable
fun PermissionScreen(onBackClicked: () -> Unit, viewModel: PermissionViewModel = hiltViewModel()) {
    var state by remember { mutableStateOf(PermissionState()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { state = viewModel.getPermissionState() }
    LaunchedEffect(Unit) { state = viewModel.getPermissionState() }
    Scaffold(topBar = { AppHeader("Permissions") }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Required Access", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Processing remains on this device. Permissions can be revoked in Android Settings.")
            Spacer(Modifier.height(16.dp))
            PermissionCard("Notification Access", "Allows the notification listener foundation to receive notification events.", state.isNotificationAccessGranted) { viewModel.openNotificationAccessSettings() }
            PermissionCard("Contacts Permission", "Allows future phonebook writing.", state.hasContactsPermission) {
                launcher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
            }
            PermissionCard("SMS Permission", "Allows sending thank you messages automatically.", state.hasSmsPermission) {
                launcher.launch(arrayOf(Manifest.permission.SEND_SMS))
            }
            PermissionCard("Logs Access", "Allows scanning for unsaved numbers in call logs and SMS.", state.hasLogPermissions) {
                launcher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_SMS))
            }
            PermissionCard("Display Over Other Apps", "Required for the Floating Guardian status bubble.", state.hasOverlayPermission) {
                viewModel.openOverlaySettings()
            }
            PermissionCard("Auto-Scan Access", "Required to detect numbers automatically while you scroll WhatsApp.", state.isAccessibilityEnabled) {
                viewModel.openAccessibilitySettings()
            }
            PermissionCard("Storage Access", "Required to download and save videos/status to gallery.", state.hasStoragePermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES))
                } else {
                    launcher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                PermissionCard("App Notifications", "Allows showing local alerts when contacts are saved.", state.hasNotifPermission) {
                    launcher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.openNotificationAccessSettings() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Refresh Listener (Off/On)")
            }

            Spacer(Modifier.height(24.dp))
            Button(onBackClicked, Modifier.fillMaxWidth()) { Text("Return to Dashboard") }
        }
    }
}
