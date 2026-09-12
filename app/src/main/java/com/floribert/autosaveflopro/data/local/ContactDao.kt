package com.floribert.autosaveflopro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM detected_contacts ORDER BY detectedAtTimestamp DESC")
    fun getAllDetectedContacts(): Flow<List<DetectedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetectedContact(contact: DetectedContactEntity): Long

    @Update
    suspend fun updateDetectedContact(contact: DetectedContactEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM detected_contacts WHERE phoneNumber = :phoneNumber)")
    suspend fun isPhoneNumberDetected(phoneNumber: String): Boolean

    @Query("SELECT COUNT(*) FROM detected_contacts")
    fun getDetectedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM detected_contacts WHERE status = 'SAVED'")
    fun getSavedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM detected_contacts WHERE detectedAtTimestamp >= :startOfDay")
    fun getTodayCount(startOfDay: Long): Flow<Int>

    // Vouchers
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Query("SELECT * FROM vouchers WHERE code = :code LIMIT 1")
    suspend fun getVoucherByCode(code: String): VoucherEntity?

    @Query("SELECT * FROM vouchers WHERE code = :code AND isUsed = 0 LIMIT 1")
    suspend fun getValidVoucher(code: String): VoucherEntity?

    @Query("UPDATE vouchers SET isUsed = 1 WHERE code = :code")
    suspend fun markVoucherAsUsed(code: String)
}
