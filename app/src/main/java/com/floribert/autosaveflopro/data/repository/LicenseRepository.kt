package com.floribert.autosaveflopro.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.floribert.autosaveflopro.data.local.ContactDao
import com.floribert.autosaveflopro.data.local.VoucherEntity
import com.floribert.autosaveflopro.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactDao: ContactDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    fun getExpiryTimestamp(): Long = prefs.getLong(Constants.KEY_LICENSE_EXPIRY, 0L)

    fun isLicenseActive(): Boolean {
        val expiry = getExpiryTimestamp()
        return expiry > System.currentTimeMillis()
    }

    fun activateLicense(days: Int) {
        val currentExpiry = getExpiryTimestamp()
        val startTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        val newExpiry = startTime + (days * 24 * 60 * 60 * 1000L)
        prefs.edit().putLong(Constants.KEY_LICENSE_EXPIRY, newExpiry).apply()
    }

    suspend fun checkVoucherStatus(code: String): String {
        val voucher = contactDao.getVoucherByCode(code.uppercase())
        return when {
            voucher == null -> "INVALID"
            voucher.isUsed -> "USED"
            else -> "VALID"
        }
    }

    suspend fun validateAndUseVoucher(code: String): String {
        val status = checkVoucherStatus(code)
        if (status == "VALID") {
            val voucher = contactDao.getValidVoucher(code.uppercase())
            if (voucher != null) {
                activateLicense(voucher.durationDays)
                contactDao.markVoucherAsUsed(code.uppercase())
                return "SUCCESS"
            }
        }
        return status
    }

    suspend fun generateVoucher(days: Int): String {
        val secureRandom = java.security.SecureRandom()
        val code = StringBuilder()
        repeat(20) {
            code.append(secureRandom.nextInt(10))
        }
        val finalCode = code.toString()
        contactDao.insertVoucher(VoucherEntity(finalCode, days))
        return finalCode
    }

    fun isTrialAvailable(): Boolean = !prefs.getBoolean(Constants.KEY_IS_TRIAL_USED, false)

    fun useTrial() {
        if (isTrialAvailable()) {
            // Badala ya kuwapa siku 1, sasa tunarekodi kuwa wametumia Trial ya Namba 10
            prefs.edit().putBoolean(Constants.KEY_IS_TRIAL_USED, true).apply()
            prefs.edit().putInt("trial_saved_count", 0).apply()
        }
    }

    fun incrementTrialCount() {
        val current = prefs.getInt("trial_saved_count", 0)
        prefs.edit().putInt("trial_saved_count", current + 1).apply()
    }

    fun isTrialExpired(): Boolean {
        if (!prefs.getBoolean(Constants.KEY_IS_TRIAL_USED, false)) return false // Haajatumia kabisa bado
        val count = prefs.getInt("trial_saved_count", 0)
        return count >= 10
    }

    fun getRemainingDays(): Int {
        val diff = getExpiryTimestamp() - System.currentTimeMillis()
        return if (diff > 0) (diff / (24 * 60 * 60 * 1000L)).toInt() else 0
    }

    // SMS Settings
    fun isSmsEnabled(): Boolean = prefs.getBoolean(Constants.KEY_SMS_ENABLED, false)
    fun setSmsEnabled(enabled: Boolean) = prefs.edit().putBoolean(Constants.KEY_SMS_ENABLED, enabled).apply()
    
    fun getSmsMessage(): String = prefs.getString(Constants.KEY_SMS_MESSAGE, Constants.DEFAULT_SMS_MESSAGE) ?: Constants.DEFAULT_SMS_MESSAGE
    fun setSmsMessage(message: String) = prefs.edit().putString(Constants.KEY_SMS_MESSAGE, message).apply()

    // Security
    fun isAppLockEnabled(): Boolean = prefs.getBoolean(Constants.KEY_APP_LOCK_ENABLED, false)
    fun setAppLockEnabled(enabled: Boolean) = prefs.edit().putBoolean(Constants.KEY_APP_LOCK_ENABLED, enabled).apply()
    
    fun getAppPin(): String = prefs.getString(Constants.KEY_APP_PIN, "") ?: ""
    fun setAppPin(pin: String) = prefs.edit().putString(Constants.KEY_APP_PIN, pin).apply()

    // Admin & Token Security
    fun getAdminPin(): String = prefs.getString("admin_pin", Constants.ADMIN_PIN) ?: Constants.ADMIN_PIN
    fun setAdminPin(pin: String) = prefs.edit().putString("admin_pin", pin).apply()

    fun getTokenGenPin(): String = prefs.getString("token_gen_pin", "0000") ?: "0000"
    fun setTokenGenPin(pin: String) = prefs.edit().putString("token_gen_pin", pin).apply()

    // Theme & Notifications
    fun getThemeMode(): Int = prefs.getInt(Constants.KEY_THEME_MODE, 0)
    fun setThemeMode(mode: Int) = prefs.edit().putInt(Constants.KEY_THEME_MODE, mode).apply()
    
    fun isInAppNotifEnabled(): Boolean = prefs.getBoolean(Constants.KEY_IN_APP_NOTIF, true)
    fun setInAppNotifEnabled(enabled: Boolean) = prefs.edit().putBoolean(Constants.KEY_IN_APP_NOTIF, enabled).apply()

    // Contact Saving Template
    fun getContactTemplate(): String = prefs.getString(Constants.KEY_CONTACT_TEMPLATE, Constants.DEFAULT_CONTACT_TEMPLATE) ?: Constants.DEFAULT_CONTACT_TEMPLATE
    fun setContactTemplate(template: String) = prefs.edit().putString(Constants.KEY_CONTACT_TEMPLATE, template).apply()

    fun isAutoScanEnabled(): Boolean = prefs.getBoolean(Constants.KEY_AUTO_SCAN_ENABLED, false)
    fun setAutoScanEnabled(enabled: Boolean) = prefs.edit().putBoolean(Constants.KEY_AUTO_SCAN_ENABLED, enabled).apply()

    // Business Bio
    fun getBioName(): String = prefs.getString(Constants.KEY_BIO_NAME, "My Business") ?: "My Business"
    fun setBioName(name: String) = prefs.edit().putString(Constants.KEY_BIO_NAME, name).apply()

    fun getBioTitle(): String = prefs.getString(Constants.KEY_BIO_TITLE, "Best Service Provider") ?: "Best Business"
    fun setBioTitle(title: String) = prefs.edit().putString(Constants.KEY_BIO_TITLE, title).apply()

    // WhatsApp Auto-Reply
    fun isWaReplyEnabled(): Boolean = prefs.getBoolean(Constants.KEY_WHATSAPP_REPLY_ENABLED, false)
    fun setWaReplyEnabled(enabled: Boolean) = prefs.edit().putBoolean(Constants.KEY_WHATSAPP_REPLY_ENABLED, enabled).apply()

    fun getWaReplyMsg(): String = prefs.getString(Constants.KEY_WHATSAPP_REPLY_MSG, Constants.DEFAULT_WA_REPLY) ?: Constants.DEFAULT_WA_REPLY
    fun setWaReplyMsg(msg: String) = prefs.edit().putString(Constants.KEY_WHATSAPP_REPLY_MSG, msg).apply()
}
