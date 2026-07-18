package com.otpbox.data.settings

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivacyStore(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "privacy_consent_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun isAgreedSync(): Boolean = prefs.getBoolean(KEY_AGREED, false)

    suspend fun isAgreed(): Boolean = withContext(Dispatchers.IO) {
        prefs.getBoolean(KEY_AGREED, false)
    }

    suspend fun setAgreed() = withContext(Dispatchers.IO) {
        prefs.edit().putBoolean(KEY_AGREED, true).apply()
    }

    companion object {
        private const val KEY_AGREED = "privacy_consent_agreed"
    }
}
