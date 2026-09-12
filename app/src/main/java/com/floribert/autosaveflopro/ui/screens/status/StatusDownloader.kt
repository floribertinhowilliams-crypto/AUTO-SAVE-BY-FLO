package com.floribert.autosaveflopro.ui.screens.status

import android.content.Context
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.floribert.autosaveflopro.ui.components.AppHeader
import com.floribert.autosaveflopro.ui.components.EmptyState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

import android.content.ContentValues
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream

data class StatusMedia(val uri: Uri, val isVideo: Boolean, val name: String)

@HiltViewModel
class StatusViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    var statuses by mutableStateOf<List<StatusMedia>>(emptyList())
    var isLoading by mutableStateOf(false)

    fun loadStatuses(treeUri: Uri) {
        isLoading = true
        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
        val files: Array<DocumentFile> = documentFile?.listFiles() ?: emptyArray()

        statuses = files.filter { file ->
            val name = file.name?.lowercase() ?: ""
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".mp4") || name.endsWith(".mov")
        }.map { file ->
            val name = file.name ?: "Unknown"
            StatusMedia(file.uri, name.lowercase().endsWith(".mp4") || name.lowercase().endsWith(".mov"), name)
        }.reversed() // Show newest first

        isLoading = false
    }

    fun downloadStatus(uri: Uri, fileName: String) {
        try {
            val contentResolver = context.contentResolver
            val isVideo = fileName.lowercase().endsWith(".mp4") || fileName.lowercase().endsWith(".mov")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "AutoSave_${System.currentTimeMillis()}_$fileName")
                put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/AutoSave" else "Pictures/AutoSave")
                }
            }

            val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val targetUri = contentResolver.insert(collection, contentValues)

            if (targetUri != null) {
                contentResolver.openInputStream(uri)?.use { input ->
                    contentResolver.openOutputStream(targetUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun StatusDownloaderScreen(onBackClicked: () -> Unit, viewModel: StatusViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedUri = it
            viewModel.loadStatuses(it)
        }
    }

    // Auto-load if permissions exist
    LaunchedEffect(Unit) {
        val persisted = context.contentResolver.persistedUriPermissions
        if (persisted.isNotEmpty()) {
            val uri = persisted[0].uri
            selectedUri = uri
            viewModel.loadStatuses(uri)
        }
    }

    Scaffold(topBar = { AppHeader("WhatsApp Status") }) { p ->
        Column(Modifier.fillMaxSize().padding(p)) {
            if (selectedUri == null) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("WhatsApp status protection requires folder access.", textAlign = TextAlign.Center)
                        Text("Please select: Android > media > com.whatsapp > WhatsApp > Media > .Statuses",
                            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { launcher.launch(null) }) {
                            Text("Grant Access to .Statuses Folder")
                        }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Folda Iliyounganishwa Sahihi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { launcher.launch(null) }) {
                        Text("Badilisha Folda (Reset)")
                    }
                }

                if (viewModel.statuses.isEmpty()) {
                    EmptyState("No Statuses found", icon = Icons.Default.Image, subtitle = "Open WhatsApp first to load statuses.")
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), Modifier.fillMaxSize().padding(8.dp)) {
                        items(viewModel.statuses) { status ->
                            StatusItem(status, onDownload = { viewModel.downloadStatus(status.uri, status.name) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusItem(status: StatusMedia, onDownload: () -> Unit) {
    Card(Modifier.padding(4.dp).aspectRatio(1f)) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (status.isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                    null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    if (status.isVideo) "Video Status" else "Image Status",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(
                onClick = onDownload,
                modifier = Modifier.align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
            ) {
                Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
