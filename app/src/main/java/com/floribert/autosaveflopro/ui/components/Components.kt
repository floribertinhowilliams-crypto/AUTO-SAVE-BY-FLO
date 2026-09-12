package com.floribert.autosaveflopro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.floribert.autosaveflopro.data.model.DetectedContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppHeader(title: String) = TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) })

@Composable
fun StatusCard(isActive: Boolean, modifier: Modifier = Modifier) {
    val c = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(c)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (isActive) "AutoSave is Active" else "Complete Setup to Activate AutoSave", fontWeight = FontWeight.Bold)
                Text(if (isActive) "Listening for supported messaging notifications." else "Grant notification access and contacts permission.")
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: Int, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickActionCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, null)
        }
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector = Icons.Default.Info, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, textAlign = TextAlign.Center)
        if (subtitle != null) Text(subtitle, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ContactCard(contact: DetectedContact) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(contact.phoneNumber, fontWeight = FontWeight.Bold)
                contact.senderName?.takeIf { it.isNotBlank() }?.let { Text("Sender: $it") }
                Text("Source: ${contact.sourceApp}", style = MaterialTheme.typography.labelSmall)
            }
            Text(contact.status.name, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable fun SectionHeader(title: String) {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp, 16.dp, 4.dp, 8.dp))
}

@Composable
fun SettingRow(title: String, subtitle: String? = null, value: String? = null, isComingSoon: Boolean = false, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).then(if (onClick != null && !isComingSoon) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            when {
                isComingSoon -> Text("Coming soon", color = MaterialTheme.colorScheme.secondary)
                value != null -> Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PermissionCard(title: String, description: String, isGranted: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning, null)
        }
    }
}
