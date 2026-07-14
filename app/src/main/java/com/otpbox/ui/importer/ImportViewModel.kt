package com.otpbox.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.data.backup.BackupEncryptor
import com.otpbox.data.backup.BackupEncryptor.WrongPasswordException
import com.otpbox.data.backup.JsonBackupImporter
import com.otpbox.domain.parse.OtpParser
import com.otpbox.data.repo.OtpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val message: String? = null,
    val isError: Boolean = false,
    val needPassword: Boolean = false
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: OtpRepository,
    private val encryptor: BackupEncryptor
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var pendingText: String? = null

    fun importJson(text: String) {
        when (val result = JsonBackupImporter.import(text)) {
            is JsonBackupImporter.Result.Success -> viewModelScope.launch {
                repository.addAll(result.entries)
                _state.value = ImportUiState("Imported ${result.entries.size} account(s)")
            }
            is JsonBackupImporter.Result.Encrypted -> {
                pendingText = text
                _state.value = ImportUiState(needPassword = true)
            }
            is JsonBackupImporter.Result.Error ->
                _state.value = ImportUiState(result.message, isError = true)
        }
    }

    fun importEncrypted(password: String) {
        val text = pendingText ?: return
        val content = try {
            encryptor.decrypt(text, password)
        } catch (e: WrongPasswordException) {
            _state.value = ImportUiState("解密失败：备份密码错误或文件已损坏", isError = true)
            return
        } catch (e: Exception) {
            _state.value = ImportUiState("解密失败：${e.message ?: "未知错误"}", isError = true)
            return
        }
        val entries = content.entries.filter { it.type.equals("TOTP", ignoreCase = true) && !it.deleted }
        viewModelScope.launch {
            repository.addAll(entries)
            pendingText = null
            _state.value = ImportUiState("Imported ${entries.size} account(s)")
        }
    }

    fun importQrRaw(raw: String) {
        when (val result = OtpParser.parse(raw)) {
            is OtpParser.Result.Success -> viewModelScope.launch {
                repository.addAll(result.entries)
                _state.value = ImportUiState("Imported ${result.entries.size} account(s)")
            }
            is OtpParser.Result.Error ->
                _state.value = ImportUiState(result.message, isError = true)
        }
    }

    fun setError(message: String) {
        _state.value = ImportUiState(message, isError = true)
    }
}
