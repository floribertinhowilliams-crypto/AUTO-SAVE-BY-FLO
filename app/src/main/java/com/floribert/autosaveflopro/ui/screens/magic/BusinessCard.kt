package com.floribert.autosaveflopro.ui.screens.magic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import com.floribert.autosaveflopro.ui.components.AppHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BioViewModel @Inject constructor(
    private val licenseRepository: LicenseRepository
) : ViewModel() {
    var name by mutableStateOf(licenseRepository.getBioName())
    var title by mutableStateOf(licenseRepository.getBioTitle())

    fun save(newName: String, newTitle: String) {
        licenseRepository.setBioName(newName)
        licenseRepository.setBioTitle(newTitle)
        name = newName
        title = newTitle
    }
}

@Composable
fun BusinessCardScreen(onBackClicked: () -> Unit, viewModel: BioViewModel = hiltViewModel()) {
    var nameInput by remember { mutableStateOf(viewModel.name) }
    var titleInput by remember { mutableStateOf(viewModel.title) }

    Scaffold(topBar = { AppHeader("Digital Business Card") }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Preview Card
            Card(
                Modifier.fillMaxWidth().height(200.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D47A1))))) {
                    Column(Modifier.padding(24.dp).align(Alignment.CenterStart)) {
                        Text(nameInput, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(titleInput, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.8f))
                        Spacer(Modifier.height(16.dp))
                        Text("AutoSave Verified Business", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                    }
                    Icon(Icons.Default.Business, null, Modifier.size(80.dp).align(Alignment.CenterEnd).padding(end = 16.dp), tint = Color.White.copy(0.2f))
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Your Name / Business Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Business Specialty / Title") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(24.dp))
            
            Button(onClick = { viewModel.save(nameInput, titleInput) }, Modifier.fillMaxWidth()) {
                Text("Update Digital Card")
            }
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedButton(onClick = { /* Share Logic */ }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("Share with Customers")
            }
            
            Spacer(Modifier.weight(1f))
            Button(onClick = onBackClicked, Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors()) {
                Text("Back to Dashboard")
            }
        }
    }
}
