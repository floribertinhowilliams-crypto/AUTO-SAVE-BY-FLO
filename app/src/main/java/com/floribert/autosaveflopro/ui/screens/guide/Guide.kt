package com.floribert.autosaveflopro.ui.screens.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floribert.autosaveflopro.ui.components.AppHeader

@Composable
fun GuideScreen(onBackClicked: () -> Unit) {
    Scaffold(topBar = { AppHeader("User Guide / Mwongozo") }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Jinsi ya Kutumia App (Swahili)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            GuideItem(1, "Ruhusu Permissions", "Nenda kwenye 'Manage Permissions' na uwashe zote (Notification, Contacts, na Logs). Bila hizi, app haiwezi kusave namba.", Icons.Default.Security)
            GuideItem(2, "Anza Kutumia (Trial)", "Nenda kwenye 'Account & Subscription' na ugonge 'Start 1 Day Free Trial' ili uanze kuitumia app bure kwa siku ya kwanza.", Icons.Default.Timer)
            GuideItem(3, "Save Namba za Zamani", "Gusa 'SCAN MANUAL' kwenye Dashboard. Itatafuta namba zote za zamani kwenye simu yako. Baada ya hapo, nenda 'History' na ugonge 'Sync All Pending'.", Icons.Default.Search)
            GuideItem(4, "Kusave Namba Mpya", "Kila mteja mpya atakayekutumia message WhatsApp, app itam-save kiotomatiki kwa jina uliloweka kwenye Settings.", Icons.Default.AutoAwesome)
            GuideItem(5, "Tuma Message bila Kusave", "Tumia 'Direct Chat' kutuma ujumbe kwa mtu yeyote WhatsApp bila kulazimika kusave namba yake kwanza.", Icons.Default.Message)
            GuideItem(6, "Hifadhi Status", "Tumia 'Status Saver' kuona na kudownload picha au video za status za marafiki zako.", Icons.Default.PhotoLibrary)

            Divider(Modifier.padding(vertical = 24.dp))

            Text("How to Use (English)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            Text("1. Grant all permissions in the 'Manage Permissions' section.\n" +
                 "2. Start your Free Trial in the 'Subscription' area.\n" +
                 "3. Use 'SCAN MANUAL' to detect old unsaved numbers from logs.\n" +
                 "4. New WhatsApp messages will trigger Auto-Save automatically.\n" +
                 "5. Use 'Direct Chat' to message unsaved numbers instantly.",
                style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(32.dp))
            Button(onClick = onBackClicked, Modifier.fillMaxWidth()) {
                Text("Back to Dashboard")
            }
        }
    }
}

@Composable
fun GuideItem(step: Int, title: String, desc: String, icon: ImageVector) {
    Row(Modifier.padding(vertical = 8.dp)) {
        Surface(
            Modifier.size(40.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(step.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall)
        }
    }
}
