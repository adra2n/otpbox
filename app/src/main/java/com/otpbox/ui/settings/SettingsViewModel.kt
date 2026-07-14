package com.otpbox.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.data.backup.BackupContent
import com.otpbox.data.backup.BackupEncryptor
import com.otpbox.data.crypto.SecurePrefs
import com.otpbox.data.repo.OtpRepository
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.data.sync.SyncManager
import com.otpbox.data.sync.SyncResult
import com.otpbox.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val appLockEnabled: Boolean = false,
    val secureScreen: Boolean = true,
    val autoLockSeconds: Int = 0,
    val pinSet: Boolean = false,
    val hasBackupPassword: Boolean = false,
    val hasGithubToken: Boolean = false,
    val gistId: String = "",
    val lastSyncAt: Long = 0,
    val syncing: Boolean = false,
    val message: String? = null,
    val pendingExport: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val securePrefs: SecurePrefs,
    private val repository: OtpRepository,
    private val encryptor: BackupEncryptor,
    private val syncManager: SyncManager,
    private val pinManager: PinManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.appLockEnabled.collect { v -> _state.update { it.copy(appLockEnabled = v) } }
        }
        viewModelScope.launch {
            settings.secureScreen.collect { v -> _state.update { it.copy(secureScreen = v) } }
        }
        viewModelScope.launch {
            settings.autoLockSeconds.collect { v -> _state.update { it.copy(autoLockSeconds = v) } }
        }
        refreshSecure()
    }

    private fun refreshSecure() {
        _state.update {
            it.copy(
                pinSet = pinManager.isPinSet,
                hasBackupPassword = !securePrefs.backupPassword.isNullOrBlank(),
                hasGithubToken = !securePrefs.githubPat.isNullOrBlank(),
                gistId = securePrefs.gistId.orEmpty(),
                lastSyncAt = securePrefs.lastSyncAt
            )
        }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch { settings.setAppLockEnabled(enabled) }
    }

    fun setSecureScreen(enabled: Boolean) {
        viewModelScope.launch { settings.setSecureScreen(enabled) }
    }

    fun setAutoLockSeconds(seconds: Int) {
        viewModelScope.launch { settings.setAutoLockSeconds(seconds) }
    }

    fun setPin(pin: String) {
        val result = runCatching { pinManager.setPin(pin) }
        refreshSecure()
        _state.update {
            it.copy(message = if (result.isSuccess) "PIN 码已设置" else "PIN 码需为 4-8 位数字")
        }
    }

    fun clearPin() {
        pinManager.clearPin()
        refreshSecure()
        _state.update { it.copy(message = "PIN 码已清除") }
    }

    fun setBackupPassword(pw: String) {
        securePrefs.backupPassword = pw.ifBlank { null }
        refreshSecure()
        _state.update { it.copy(message = if (pw.isBlank()) "Backup password cleared" else "Backup password saved") }
    }

    fun setGithubToken(pat: String) {
        securePrefs.githubPat = pat.ifBlank { null }
        refreshSecure()
        _state.update { it.copy(message = "GitHub token saved") }
    }

    fun setGistId(id: String) {
        securePrefs.gistId = id.ifBlank { null }
        refreshSecure()
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun setMessage(msg: String) {
        _state.update { it.copy(message = msg) }
    }

    suspend fun exportEnvelope(): String? {
        val pw = securePrefs.backupPassword ?: return null
        val entries = repository.getAllIncludingDeleted().filter { !it.deleted }
        return encryptor.encrypt(BackupContent(entries = entries), pw)
    }

    fun exportConsumed() {
        _state.update { it.copy(pendingExport = null, message = "Backup exported") }
    }

    fun push() {
        val pw = securePrefs.backupPassword ?: return failNoPassword()
        _state.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = syncManager.push(pw)
            refreshSecure()
            _state.update { it.copy(syncing = false, message = messageOf(result)) }
        }
    }

    fun pull() {
        val pw = securePrefs.backupPassword ?: return failNoPassword()
        _state.update { it.copy(syncing = true) }
        viewModelScope.launch {
            val result = syncManager.pull(pw)
            refreshSecure()
            _state.update { it.copy(syncing = false, message = messageOf(result)) }
        }
    }

    private fun failNoPassword() {
        _state.update { it.copy(message = "Set a backup password first") }
    }

    private fun messageOf(result: SyncResult): String = when (result) {
        is SyncResult.Success -> result.message
        is SyncResult.Error -> result.message
    }
}
