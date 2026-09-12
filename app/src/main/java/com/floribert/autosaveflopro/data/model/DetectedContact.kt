package com.floribert.autosaveflopro.data.model

enum class DetectionStatus { PENDING, SAVED, IGNORED, FAILED }

data class DetectedContact(
    val id: Long = 0,
    val phoneNumber: String,
    val senderName: String?,
    val sourceApp: String,
    val detectedAtTimestamp: Long,
    val status: DetectionStatus
)
