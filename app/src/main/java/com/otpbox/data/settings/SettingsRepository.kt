package com.otpbox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "otpbox_settings")

enum class SortOrder { CUSTOM, ISSUER, ACCOUNT }

class SettingsRepository(private val context: Context) {

    private val appLockKey = booleanPreferencesKey("app_lock_enabled")
    private val sortOrderKey = stringPreferencesKey("sort_order")
    private val secureScreenKey = booleanPreferencesKey("secure_screen")
    private val autoLockSecondsKey = intPreferencesKey("auto_lock_seconds")

    val appLockEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[appLockKey] ?: false }

    /** Seconds in background before re-locking; 0 = immediately. */
    val autoLockSeconds: Flow<Int> =
        context.dataStore.data.map { it[autoLockSecondsKey] ?: 0 }

    val secureScreen: Flow<Boolean> =
        context.dataStore.data.map { it[secureScreenKey] ?: true }

    val sortOrder: Flow<SortOrder> =
        context.dataStore.data.map {
            runCatching { SortOrder.valueOf(it[sortOrderKey] ?: SortOrder.CUSTOM.name) }
                .getOrDefault(SortOrder.CUSTOM)
        }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[appLockKey] = enabled }
    }

    suspend fun setAutoLockSeconds(seconds: Int) {
        context.dataStore.edit { it[autoLockSecondsKey] = seconds }
    }

    suspend fun setSecureScreen(enabled: Boolean) {
        context.dataStore.edit { it[secureScreenKey] = enabled }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[sortOrderKey] = order.name }
    }
}
