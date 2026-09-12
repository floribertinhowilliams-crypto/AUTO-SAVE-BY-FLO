package com.floribert.autosaveflopro.ui.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floribert.autosaveflopro.data.model.DetectedContact
import com.floribert.autosaveflopro.data.repository.ContactRepository
import com.floribert.autosaveflopro.ui.components.AppHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BulkSmsViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {
    val contacts = repository.getAllDetectedContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun BulkSmsScreen(onBackClicked: () -> Unit, viewModel: BulkSmsViewModel = hiltViewModel()) {
    val list by viewModel.contacts.collectAsState()
    var message by remember { mutableStateOf("") }
    val selectedContacts = remember { mutableStateListOf<DetectedContact>() }

    Scaffold(topBar = { AppHeader("Bulk SMS Sender") }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(16.dp)) {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Broadcast Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            Spacer(Modifier.height(16.dp))
            Text("Select Contacts (${selectedContacts.size})", style = MaterialTheme.typography.titleMedium)
            
            LazyColumn(Modifier.weight(1f)) {
                items(list) { contact ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Checkbox(
                            checked = selectedContacts.contains(contact),
                            onCheckedChange = { 
                                if (it) selectedContacts.add(contact) else selectedContacts.remove(contact)
                            }
                        )
                        Column {
                            Text(contact.phoneNumber)
                            Text(contact.senderName ?: "Unknown", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            Button(
                onClick = { /* Phase 04: Bulk Sending Logic */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedContacts.isNotEmpty() && message.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null)
                Spacer(Modifier.width(8.dp))
                Text("Send to ${selectedContacts.size} Contacts")
            }
        }
    }
}
