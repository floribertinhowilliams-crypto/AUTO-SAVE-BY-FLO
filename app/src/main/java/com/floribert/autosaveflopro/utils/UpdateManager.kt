package com.floribert.autosaveflopro.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.floribert.autosaveflopro.BuildConfig
import com.floribert.autosaveflopro.data.model.UpdateInfo
import com.floribert.autosaveflopro.data.remote.UpdateApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    private val updateApi: UpdateApi,
    @ApplicationContext private val context: Context
) {
    /**
     * Checks if a new version is available online.
     */
    suspend fun checkForUpdate(url: String): UpdateInfo? {
        return try {
            val info = updateApi.getUpdateInfo(url)
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                info
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Triggers the download of the APK file.
     */
    fun downloadApk(url: String, fileName: String = "AutoSaveUpdate.apk") {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading AutoSave Pro Update")
            .setDescription("New version is being downloaded...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}
