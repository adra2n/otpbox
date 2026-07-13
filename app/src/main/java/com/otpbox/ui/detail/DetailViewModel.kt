package com.otpbox.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.domain.model.OtpEntry
import com.otpbox.data.repo.OtpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val entry: OtpEntry? = null,
    val issuer: String = "",
    val account: String = "",
    val note: String = "",
    val loading: Boolean = true,
    val done: Boolean = false
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: OtpRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            val entry = repository.getById(id)
            _state.value = DetailUiState(
                entry = entry,
                issuer = entry?.issuer.orEmpty(),
                account = entry?.account.orEmpty(),
                note = entry?.note.orEmpty(),
                loading = false
            )
        }
    }

    fun onIssuer(v: String) { _state.value = _state.value.copy(issuer = v) }
    fun onAccount(v: String) { _state.value = _state.value.copy(account = v) }
    fun onNote(v: String) { _state.value = _state.value.copy(note = v) }

    fun save() {
        val s = _state.value
        val entry = s.entry ?: return
        viewModelScope.launch {
            repository.update(
                entry.copy(
                    issuer = s.issuer.trim(),
                    account = s.account.trim(),
                    note = s.note.trim().ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            )
            _state.value = _state.value.copy(done = true)
        }
    }

    fun delete() {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            repository.delete(entry.id)
            _state.value = _state.value.copy(done = true)
        }
    }
}
