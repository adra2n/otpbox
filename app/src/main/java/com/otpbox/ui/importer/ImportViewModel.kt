package com.otpbox.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val isError: Boolean = false
)

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val repository: OtpRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun importJson(text: String) {
        when (val result = JsonBackupImporter.import(text)) {
            is JsonBackupImporter.Result.Success -> viewModelScope.launch {
                repository.addAll(result.entries)
                _state.value = ImportUiState("Imported ${result.entries.size} account(s)")
            }
            is JsonBackupImporter.Result.Error ->
                _state.value = ImportUiState(result.message, isError = true)
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
