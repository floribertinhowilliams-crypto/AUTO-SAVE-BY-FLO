package com.floribert.autosaveflopro.utils

object Constants {
    const val DATABASE_NAME = "autosave_contacts_db"
    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    // Subscription Plans
    const val PLAN_WEEKLY_PRICE = "7,500"
    const val PLAN_MONTHLY_PRICE = "21,000"
    const val PLAN_3MONTHS_PRICE = "54,000"
    const val PAYMENT_NUMBER = "0762983844"
    const val PAYMENT_NAME = "AGNES ALFRED"
    
    // License Keys
    const val PREFS_NAME = "autosave_prefs"
    const val KEY_LICENSE_EXPIRY = "license_expiry"
    const val KEY_IS_TRIAL_USED = "is_trial_used"
    const val ADMIN_PIN = "1234" // Default Admin PIN

    // SMS Settings
    const val KEY_SMS_ENABLED = "sms_enabled"
    const val KEY_SMS_MESSAGE = "sms_message"
    const val DEFAULT_SMS_MESSAGE = "Hello! Your contact has been saved by AutoSave Pro. Thank you!"

    // Security
    const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    const val KEY_APP_PIN = "app_pin"

    // Theme
    const val KEY_THEME_MODE = "theme_mode" // 0: System, 1: Light, 2: Dark
    const val KEY_IN_APP_NOTIF = "in_app_notif_enabled"

    // Business Bio
    const val KEY_BIO_NAME = "bio_name"
    const val KEY_BIO_TITLE = "bio_title"
    
    // Auto-Reply
    const val KEY_WHATSAPP_REPLY_ENABLED = "wa_reply_enabled"
    const val KEY_WHATSAPP_REPLY_MSG = "wa_reply_msg"
    const val DEFAULT_WA_REPLY = "Habari! Nimepokea ujumbe wako. Nimeku-save, karibu sana kwa huduma zetu."

    // Referral
    const val REFERRAL_LINK = "https://autosavepro.com/download?ref="

    // Contact Saving
    const val KEY_CONTACT_TEMPLATE = "contact_name_template"
    const val DEFAULT_CONTACT_TEMPLATE = "AutoSave"

    // Auto Scan
    const val KEY_AUTO_SCAN_ENABLED = "auto_scan_enabled"

    // Update URL (GitHub Raw Link)
    const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/floribertinhowilliams-crypto/AUTO-SAVE-BY-FLO/main/autosave_update.json"
}
