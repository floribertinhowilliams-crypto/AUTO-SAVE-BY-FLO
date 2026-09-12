package com.floribert.autosaveflopro.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material.icons.filled.Sync

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ContactRepository
) : ViewModel() {
    val history = repository.getAllDetectedContacts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun bulkSync(contacts: List<DetectedContact>) {
        viewModelScope.launch {
            contacts.filter { it.status == DetectionStatus.PENDING }.forEach { contact ->
                val success = repository.saveToPhonebook(contact)
                if (success) {
                    repository.updateContactStatus(contact.copy(status = DetectionStatus.SAVED))
                }
            }
        }
    }
}

@Composable fun HistoryScreen(onBackClicked: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val list by viewModel.history.collectAsState()
    Scaffold(
        topBar = { AppHeader("Detection History") },
        floatingActionButton = {
            if (list.any { it.status == DetectionStatus.PENDING }) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.bulkSync(list) },
                    icon = { Icon(Icons.Default.Sync, null) },
                    text = { Text("Sync All Pending") }
                )
            }
        }
    ) { p ->
        if (list.isEmpty()) EmptyState("No detection history yet.", Icons.Default.History, "Listening for WhatsApp notifications...", Modifier.fillMaxSize().padding(p))
        else LazyColumn(Modifier.fillMaxSize().padding(p).padding(horizontal = 16.dp)) { items(list) { ContactCard(it) } }
    }
}
