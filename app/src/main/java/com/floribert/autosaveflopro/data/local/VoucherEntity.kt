package com.floribert.autosaveflopro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey val code: String,
    val durationDays: Int,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
