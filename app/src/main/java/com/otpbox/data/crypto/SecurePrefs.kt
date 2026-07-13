package com.otpbox.data.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Encrypted key-value store for sensitive settings (GitHub PAT, gist id, etc.). */
class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "otpbox_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var githubPat: String?
        get() = prefs.getString(KEY_PAT, null)
        set(value) = prefs.edit().putString(KEY_PAT, value).apply()

    var gistId: String?
        get() = prefs.getString(KEY_GIST_ID, null)
        set(value) = prefs.edit().putString(KEY_GIST_ID, value).apply()

    var backupPasswordHint: String?
        get() = prefs.getString(KEY_BACKUP_HINT, null)
        set(value) = prefs.edit().putString(KEY_BACKUP_HINT, value).apply()

    /** Backup/sync password, stored encrypted on-device; never sent to the gist. */
    var backupPassword: String?
        get() = prefs.getString(KEY_BACKUP_PW, null)
        set(value) = prefs.edit().putString(KEY_BACKUP_PW, value).apply()

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    /** PBKDF2 "salt:hash" of the backup PIN; null when no PIN is set. */
    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    companion object {
        private const val KEY_PAT = "github_pat"
        private const val KEY_GIST_ID = "gist_id"
        private const val KEY_BACKUP_HINT = "backup_password_hint"
        private const val KEY_BACKUP_PW = "backup_password"
        private const val KEY_LAST_SYNC = "last_sync_at"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
