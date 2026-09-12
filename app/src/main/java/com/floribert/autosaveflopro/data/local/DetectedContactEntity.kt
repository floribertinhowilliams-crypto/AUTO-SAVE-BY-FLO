package com.floribert.autosaveflopro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_contacts")
data class DetectedContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val senderName: String?,
    val sourceApp: String,
    val detectedAtTimestamp: Long,
    val status: String
)
