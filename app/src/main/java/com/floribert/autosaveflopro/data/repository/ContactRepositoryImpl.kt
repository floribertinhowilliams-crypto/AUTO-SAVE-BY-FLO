package com.floribert.autosaveflopro.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.floribert.autosaveflopro.data.local.ContactDao
import com.floribert.autosaveflopro.data.local.DetectedContactEntity
import com.floribert.autosaveflopro.data.model.DetectedContact
import com.floribert.autosaveflopro.data.model.DetectionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    @ApplicationContext private val context: Context,
    private val licenseRepository: LicenseRepository
) : ContactRepository {
    override fun getAllDetectedContacts(): Flow<List<DetectedContact>> =
        contactDao.getAllDetectedContacts().map { list -> list.map { it.toDomain() } }

    override fun getDetectedCount(): Flow<Int> = contactDao.getDetectedCount()

    override suspend fun saveDetectedContact(contact: DetectedContact): Long =
        contactDao.insertDetectedContact(contact.toEntity())

    override suspend fun updateContactStatus(contact: DetectedContact) =
        contactDao.updateDetectedContact(contact.toEntity())

    override suspend fun isAlreadyRecorded(phoneNumber: String): Boolean =
        contactDao.isPhoneNumberDetected(phoneNumber)

    override suspend fun saveToPhonebook(contact: DetectedContact): Boolean {
        return try {
            val ops = arrayListOf<ContentProviderOperation>()

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build())

            val template = licenseRepository.getContactTemplate()
            val name = if (!contact.senderName.isNullOrBlank()) {
                contact.senderName
            } else {
                "$template ${contact.phoneNumber.takeLast(4)}".trim()
            }

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build())

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build())

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun scanAndSaveUnnamedContacts(): Int {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var count = 0
                val unsavedNumbers = mutableSetOf<String>()

                // Check Permissions first
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return@withContext -1 // Indicate permission error
                }

                // 1. Scan Call Log
                val callLogUri = android.provider.CallLog.Calls.CONTENT_URI
                try {
                    context.contentResolver.query(callLogUri, arrayOf(android.provider.CallLog.Calls.NUMBER), null, null, null)?.use { cursor ->
                        val numIdx = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                        if (numIdx != -1) {
                            while (cursor.moveToNext()) {
                                val number = cursor.getString(numIdx)
                                if (!number.isNullOrBlank() && number.length >= 8 && !isNumberInContacts(number)) {
                                    unsavedNumbers.add(number.replace(Regex("""[\s-]"""), ""))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Scan SMS
                val smsUri = android.net.Uri.parse("content://sms/inbox")
                try {
                    context.contentResolver.query(smsUri, arrayOf("address"), null, null, null)?.use { cursor ->
                        val addrIdx = cursor.getColumnIndex("address")
                        if (addrIdx != -1) {
                            while (cursor.moveToNext()) {
                                val address = cursor.getString(addrIdx)
                                if (!address.isNullOrBlank() && address.any { it.isDigit() } && address.length >= 8 && !isNumberInContacts(address)) {
                                    unsavedNumbers.add(address.replace(Regex("""[\s-]"""), ""))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. Ultra Deep WhatsApp System Account Scan
                val whatsappTypes = arrayOf("com.whatsapp", "com.whatsapp.w4b")
                whatsappTypes.forEach { type ->
                    val selection = "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?"
                    val selectionArgs = arrayOf(type)
                    
                    context.contentResolver.query(
                        ContactsContract.Data.CONTENT_URI, 
                        null, 
                        selection, 
                        selectionArgs, 
                        null
                    )?.use { cursor ->
                        val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                        
                        while (cursor.moveToNext()) {
                            val rawData = if (data1Idx != -1) cursor.getString(data1Idx) else null
                            
                            if (!rawData.isNullOrBlank()) {
                                val number = rawData.substringBefore("@").replace(Regex("""[^\d]"""), "")
                                if (number.length >= 8 && !isNumberInContacts(number)) {
                                    unsavedNumbers.add(number)
                                }
                            }
                        }
                    }
                }

                // 4. Save to Local DB and automatically save to phonebook
                unsavedNumbers.forEach { number ->
                    if (!isAlreadyRecorded(number)) {
                        val contact = DetectedContact(
                            phoneNumber = number,
                            senderName = null,
                            sourceApp = "Deep Scan",
                            detectedAtTimestamp = System.currentTimeMillis(),
                            status = DetectionStatus.PENDING
                        )
                        val id = saveDetectedContact(contact)
                        
                        // Automatically push this old contact to the phonebook
                        val success = saveToPhonebook(contact.copy(id = id))
                        if (success) {
                            updateContactStatus(contact.copy(id = id, status = DetectionStatus.SAVED))
                        } else {
                            updateContactStatus(contact.copy(id = id, status = DetectionStatus.FAILED))
                        }
                        count++
                    }
                }
                count
            } catch (e: Exception) {
                e.printStackTrace()
                -2 // Indicate unknown error
            }
        }
    }

    override fun isNumberInContacts(phoneNumber: String): Boolean {
        // ZIMA KWA MUDA UKAGUZI HUU ILI KURUHUSU NAMBA ZOTE ZIINGIE BILA KUZUIWA NA MFUMO WA ANDROID
        return false
    }

    private fun DetectedContactEntity.toDomain() = DetectedContact(
        id, phoneNumber, senderName, sourceApp, detectedAtTimestamp,
        runCatching { DetectionStatus.valueOf(status) }.getOrDefault(DetectionStatus.PENDING)
    )

    private fun DetectedContact.toEntity() = DetectedContactEntity(
        id, phoneNumber, senderName, sourceApp, detectedAtTimestamp, status.name
    )
}
