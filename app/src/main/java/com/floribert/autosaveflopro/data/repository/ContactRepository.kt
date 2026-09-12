package com.floribert.autosaveflopro.data.repository

import com.floribert.autosaveflopro.data.model.DetectedContact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getAllDetectedContacts(): Flow<List<DetectedContact>>
    fun getDetectedCount(): Flow<Int>
    fun getSavedCount(): Flow<Int>
    fun getTodayCount(startOfDay: Long): Flow<Int>
    suspend fun saveDetectedContact(contact: DetectedContact): Long
    suspend fun updateContactStatus(contact: DetectedContact)
    suspend fun isAlreadyRecorded(phoneNumber: String): Boolean
    suspend fun saveToPhonebook(contact: DetectedContact): Boolean
    suspend fun scanAndSaveUnnamedContacts(): Int
    fun isNumberInContacts(phoneNumber: String): Boolean
}
