package com.floribert.autosaveflopro.ui.screens.downloader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.floribert.autosaveflopro.ui.components.AppHeader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val EXTRACT_API_BASE = "https://acp-video-extractor.vercel.app" // TAFADHALI: Badilisha na URL yako halisi

@Composable
fun VideoDownloaderScreen(onBackClicked: () -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Warning if API is not set up
    LaunchedEffect(Unit) {
        if (EXTRACT_API_BASE.contains("YOUR-PROJECT")) {
            statusMessage = "Admin: Tafadhali weka URL sahihi ya Vercel Server kwenye kodi."
        }
    }

    Scaffold(topBar = { AppHeader("Premium Video Downloader") }) { p ->
        Column(
            Modifier.fillMaxSize().padding(p).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Social Media Video Downloader",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Paste links from TikTok, Instagram, or YouTube. ACP will attempt to extract the high-quality video file.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Paste Video Link Here") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://...") },
                singleLine = true
            )

            if (statusMessage.isNotBlank()) {
                Card(
                    Modifier.padding(top = 16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (statusMessage.contains("Error") || statusMessage.contains("Invalid"))
                            MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        statusMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
            // Check for direct video links (.mp4, .mov, etc) early without calling server
            val isDirectVideo = urlInput.lowercase().let {
                it.contains(".mp4") || it.contains(".mov") || it.contains(".webm")
            }

            if (urlInput.isBlank() || !urlInput.startsWith("http")) {
                statusMessage = "Invalid URL. Please paste a valid link."
            } else if (!com.floribert.autosaveflopro.utils.PermissionUtils.hasStoragePermission(context)) {
                statusMessage = "Permission Denied. Please grant Storage Access in Manage Permissions."
            } else if (isDirectVideo) {
                // If it's a direct video link, download it immediately without server extraction
                isDownloading = true
                statusMessage = "Direct video link detected. Starting download..."
                downloadFile(context, urlInput, urlInput)
                statusMessage = "Download started in background!"
                urlInput = ""
                isDownloading = false
            } else {
                isDownloading = true
                statusMessage = "Analyzing link and extracting video source..."

                scope.launch {
                    val result = extractVideoUrlFromServer(urlInput)
                    result.onSuccess { res ->
                        downloadFile(context, res.videoUrl, res.referer)
                        statusMessage = "Extraction Successful! Download started in background."
                        urlInput = ""
                    }.onFailure { error ->
                        statusMessage = "Error: ${error.message}"
                    }
                    isDownloading = false
                }
            }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isDownloading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Extract & Download")
                }
            }

            Spacer(Modifier.height(32.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📢 Notice:", fontWeight = FontWeight.Bold)
                    Text("For YouTube/Instagram, ACP tries to find the raw .mp4 file. If the video is '0 seconds', the link provider blocked the extraction. Files are saved in 'Movies/AutoSave'.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.weight(1f))
            Button(onClick = onBackClicked, Modifier.fillMaxWidth(), colors = ButtonDefaults.filledTonalButtonColors()) {
                Text("Back to Dashboard")
            }
        }
    }
}

data class VideoExtractResult(
    val videoUrl: String,
    val referer: String
)

private suspend fun extractVideoUrlFromServer(originalUrl: String): Result<VideoExtractResult> =
    withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("$EXTRACT_API_BASE/api/extract-video")
            val connection = endpoint.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 25000
            connection.setRequestProperty("Content-Type", "application/json")

            val requestBody = JSONObject().apply {
                put("url", originalUrl)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                if (err.isNullOrBlank()) "{\"success\":false, \"error\":\"Server error: $responseCode\"}" else err
            }
            connection.disconnect()

            if (responseText.trim().startsWith("<")) {
                return@withContext Result.failure(Exception("Server ilirudisha ukurasa wa HTML badala ya data. Huenda URL ya server siyo sahihi au server ina matatizo."))
            }

            val json = try {
                JSONObject(responseText)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Jibu la server siyo JSON halali. Jibu: ${responseText.take(50)}..."))
            }

            val success = json.optBoolean("success", false)

            if (!success) {
                val errorMsg = json.optString("error", "Imeshindwa kupata video - jaribu link nyingine au server ina tatizo.")
                return@withContext Result.failure(Exception(errorMsg))
            }

            if (videoUrl.isBlank()) {
                return@withContext Result.failure(Exception("Server haikurudisha video URL"))
            }

            Result.success(VideoExtractResult(videoUrl, referer))
        } catch (e: Exception) {
            Result.failure(Exception("Imeshindwa kuunganisha na server: ${e.message}"))
        }
    }

private fun downloadFile(context: Context, url: String, referer: String) {
    val cleanUrl = url.trim()

    val request = try {
        DownloadManager.Request(Uri.parse(cleanUrl))
    } catch (e: Exception) {
        Toast.makeText(context, "Invalid Video Link", Toast.LENGTH_SHORT).show()
        return
    }

    // Headers za kitaalamu — Referer NDIYO iliyokosekana hapo awali
    request.addRequestHeader(
        "User-Agent",
        "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36"
    )
    request.addRequestHeader("Accept", "video/mp4,video/*;q=0.9,image/webp,image/apng,*/*;q=0.8")
    request.addRequestHeader("Referer", referer)

    val fileName = when {
        cleanUrl.contains("tiktok") -> "TikTok_Video_${System.currentTimeMillis()}.mp4"
        cleanUrl.contains("instagram") || referer.contains("instagram") ->
            "Insta_Video_${System.currentTimeMillis()}.mp4"
        else -> "ACP_Video_${System.currentTimeMillis()}.mp4"
    }

    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
    request.setTitle("ACP Downloader")
    request.setDescription("Saving your video...")
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "AutoSave/$fileName")

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)

    Toast.makeText(context, "Download Started... Check your notification bar.", Toast.LENGTH_LONG).show()
}
