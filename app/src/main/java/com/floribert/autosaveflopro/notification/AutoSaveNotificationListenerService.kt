package com.floribert.autosaveflopro.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.floribert.autosaveflopro.data.model.DetectedContact
import com.floribert.autosaveflopro.data.model.DetectionStatus
import com.floribert.autosaveflopro.data.repository.ContactRepository
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import com.floribert.autosaveflopro.utils.Constants
import com.floribert.autosaveflopro.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutoSaveNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var repository: ContactRepository
    @Inject lateinit var licenseRepository: LicenseRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener connected.")
    }

    private fun isGroupChat(senderName: String?): Boolean {
        if (senderName == null) return false
        val lower = senderName.lowercase()

        val groupKeywords = listOf(
            "group", "family", "team", "chat", "community", "gc", "grp",
            "班", "grupo", "groupe", "grupp"
        )

        for (keyword in groupKeywords) {
            if (lower.contains(keyword)) {
                Log.d(TAG, "Group detected by keyword: $keyword in $senderName")
                return true
            }
        }

        if (senderName.count { it == ',' } >= 2) {
            Log.d(TAG, "Group detected by multiple names: $senderName")
            return true
        }

        if (lower.endsWith(" group") || lower.endsWith(" gc") || lower.endsWith(" chat") || lower.endsWith(" team")) {
            Log.d(TAG, "Group detected by suffix: $senderName")
            return true
        }

        return false
    }

    private suspend fun saveToPhonebookWithRetry(
        contact: DetectedContact,
        maxRetries: Int = 3
    ): Boolean {
        var lastError: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                Log.d(TAG, "Saving to phonebook (attempt $attempt/$maxRetries): ${contact.phoneNumber}")
                val success = repository.saveToPhonebook(contact)

                if (success) {
                    Log.i(TAG, "✓ Phonebook save succeeded on attempt $attempt")
                    return true
                } else {
                    Log.w(TAG, "✗ Phonebook save returned false on attempt $attempt")
                    lastError = Exception("Phonebook save returned false")
                }
            } catch (e: Exception) {
                Log.w(TAG, "✗ Phonebook save failed on attempt $attempt: ${e.message}")
                lastError = e

                if (e.message?.contains("permission", ignoreCase = true) == true) {
                    Log.e(TAG, "Permission denied - no point retrying")
                    return false
                }
            }

            if (attempt < maxRetries) {
                val delayMs = (100 * attempt).toLong()
                Log.d(TAG, "Waiting ${delayMs}ms before retry...")
                kotlinx.coroutines.delay(delayMs)
            }
        }

        Log.e(TAG, "✗ All save attempts failed. Last error: ${lastError?.message}")
        return false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName != Constants.PACKAGE_WHATSAPP && packageName != Constants.PACKAGE_WHATSAPP_BUSINESS) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        Log.d(TAG, "Notification from $packageName: T='$title', B='$text', S='$subText'")

        // FIX: Filter out group chats early based on Title/SubText patterns
        if (isGroupChat(title) || isGroupChat(subText)) {
            Log.d(TAG, "✗ Skipping group chat notification: T='$title', S='$subText'")
            return
        }

        // Kwenye ma-group ya WhatsApp, ujumbe mara nyingi una muundo wa "Jina (Group): Ujumbe" au "Namba: Ujumbe"
        // Hapa tunaangalia kama ujumbe una herufi maalum za group au kama kichwa kina namba lakini text ina namba nyingine kabisa
        if (title.contains(":") || text.contains(":") || subText.contains(":")) {
            // Kama jina la mtu lina ":" linaashiria group chat kwenye mifumo mingi ya notifikeshon ya WhatsApp
            Log.d(TAG, "✗ Skipping group format message with colon indicator")
            return
        }

        // Enhanced Regex: Handles (+), spaces, and varied lengths
        val phoneRegex = Regex("""(\+?\d{9,15})""")

        var detectedNumber: String? = null
        var detectedName: String? = null

        // 1. Search for number in ALL fields
        val fullContent = "$title $text $subText".replace(Regex("""[\s-]"""), "")
        val match = phoneRegex.find(fullContent)

        if (match != null) {
            detectedNumber = match.value

            // 2. Extract Name if title is NOT the number
            val cleanTitle = title.trim()
            detectedName = when {
                cleanTitle.contains("~") -> cleanTitle.substringAfter("~").trim()
                cleanTitle.contains("@") -> cleanTitle.substringAfter("@").trim()
                cleanTitle.contains("%") -> cleanTitle.substringAfter("%").trim()
                !cleanTitle.replace(Regex("""[^\d]"""), "").contains(detectedNumber) -> cleanTitle
                else -> null
            }

            // Clean up name kutoka kwenye alama zisizotakiwa lakini tunahifadhi jina zima la mtu (pamoja na nafasi)
            detectedName = detectedName?.replace(Regex("""[^\w\s]"""), "")?.trim()
        }

        if (detectedNumber != null && detectedNumber.length >= 8) {
            handleDetectedNumber(detectedNumber, detectedName, packageName, sbn)
        }
    }

    private fun handleDetectedNumber(number: String, senderName: String?, source: String, sbn: StatusBarNotification) {
        // Debug Toast on Main Thread
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "AutoSave: Number detected $number", Toast.LENGTH_SHORT).show()
        }

        serviceScope.launch {
            val isPremiumActive = licenseRepository.isLicenseActive()
            val isTrialRunning = !isPremiumActive && !licenseRepository.isTrialAvailable() && !licenseRepository.isTrialExpired()

            if (!isPremiumActive && !isTrialRunning) {
                Log.w(TAG, "License or Trial expired. Skipping save for $number.")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "AutoSave Locked: Please renew your license!", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            if (repository.isAlreadyRecorded(number)) {
                Log.d(TAG, "Number $number already recorded in database. Skipping phonebook re-save.")
                return@launch
            }

            val contact = DetectedContact(
                phoneNumber = number,
                senderName = if (senderName != number) senderName else null,
                sourceApp = if (source.contains("w4b")) "WhatsApp Business" else "WhatsApp",
                detectedAtTimestamp = System.currentTimeMillis(),
                status = DetectionStatus.PENDING
            )

            val id = try {
                repository.saveDetectedContact(contact)
            } catch (e: Exception) {
                Log.e(TAG, "✗ Failed to save to local database: ${e.message}")
                return@launch
            }

            if (id <= 0) {
                Log.e(TAG, "✗ Database returned invalid ID: $id")
                return@launch
            }

            val savedContact = contact.copy(id = id)
            Log.d(TAG, "Saved $number to local DB (ID: $id). Attempting phonebook save...")

            if (PermissionUtils.hasContactPermissions(applicationContext)) {
                val success = saveToPhonebookWithRetry(savedContact)
                if (success) {
                    if (isTrialRunning) {
                        licenseRepository.incrementTrialCount()
                    }
                    try {
                        repository.updateContactStatus(savedContact.copy(status = DetectionStatus.SAVED))
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to update contact status to SAVED: ${e.message}")
                    }
                    Log.d(TAG, "Successfully saved $number to phonebook.")

                    if (licenseRepository.isInAppNotifEnabled()) {
                        showLocalNotification(number, senderName)
                    }

                    // SEND SMART AUTO-REPLY ON WHATSAPP
                    if (licenseRepository.isWaReplyEnabled()) {
                        attemptWhatsAppReply(sbn)
                    }

                    // Send Thank You SMS if enabled
                    if (licenseRepository.isSmsEnabled()) {
                        sendThankYouSms(number)
                    }
                } else {
                    try {
                        repository.updateContactStatus(savedContact.copy(status = DetectionStatus.FAILED))
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Failed to update contact status to FAILED: ${e.message}")
                    }
                    Log.e(TAG, "Failed to save $number to phonebook after all retries.")
                }
            } else {
                Log.w(TAG, "Missing contacts permission. Keeping as PENDING.")
            }
        }
    }

    private fun sendThankYouSms(phoneNumber: String) {
        try {
            val message = licenseRepository.getSmsMessage()
            val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                applicationContext.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "Thank you SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS: ${e.message}")
        }
    }

    private fun attemptWhatsAppReply(sbn: StatusBarNotification) {
        val actions = sbn.notification.actions ?: return
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.resultKey != null) {
                    try {
                        val replyIntent = Intent()
                        val results = Bundle()
                        results.putCharSequence(remoteInput.resultKey, licenseRepository.getWaReplyMsg())
                        RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, results)
                        action.actionIntent.send(applicationContext, 0, replyIntent)
                        Log.d(TAG, "Smart Auto-Reply sent to WhatsApp.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send auto-reply: ${e.message}")
                    }
                }
            }
        }
    }

    private fun showLocalNotification(number: String, name: String?) {
        val channelId = "autosave_notif_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Contact Saved", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("New Contact Saved!")
            .setContentText("Number: $number ${if (name != null) "($name)" else ""} was added to your phonebook.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "AutoSaveNotifService"
    }
}
