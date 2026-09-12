package com.floribert.autosaveflopro.ui.screens.saved

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floribert.autosaveflopro.data.model.DetectedContact
import com.floribert.autosaveflopro.data.model.DetectionStatus
import com.floribert.autosaveflopro.data.repository.ContactRepository
import com.floribert.autosaveflopro.ui.components.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

@HiltViewModel
class SavedViewModel @Inject constructor(repository: ContactRepository) : ViewModel() {
    val savedContacts = repository.getAllDetectedContacts()
        .map { list -> list.filter { it.status == DetectionStatus.SAVED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportToCsv(context: android.content.Context, contacts: List<DetectedContact>) {
        val csvHeader = "Phone,Name,App,Timestamp\n"
        val csvData = contacts.joinToString("\n") { 
            "${it.phoneNumber},${it.senderName ?: ""},${it.sourceApp},${it.detectedAtTimestamp}"
        }
        val fileContent = csvHeader + csvData
        
        try {
            val file = File(context.cacheDir, "saved_contacts.csv")
            file.writeText(fileContent)
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Contacts"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun SavedContactsScreen(onBackClicked: () -> Unit, viewModel: SavedViewModel = hiltViewModel()) {
    val list by viewModel.savedContacts.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { AppHeader("Saved Contacts") },
        floatingActionButton = {
            if (list.isNotEmpty()) {
                FloatingActionButton(onClick = { viewModel.exportToCsv(context, list) }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export to CSV")
                }
            }
        }
    ) { p ->
        if (list.isEmpty()) {
            EmptyState(
                "No contacts saved to device yet.",
                Icons.Default.Person,
                "Contacts will appear here after they are automatically saved.",
                Modifier.fillMaxSize().padding(p)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(p).padding(horizontal = 16.dp)) {
                item { SectionHeader("Phonebook Sync Complete") }
                items(list) { ContactCard(it) }
            }
        }
    }
}
