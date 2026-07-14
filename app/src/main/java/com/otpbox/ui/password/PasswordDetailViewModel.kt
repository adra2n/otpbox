package com.otpbox.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.data.repo.PasswordRepository
import com.otpbox.domain.model.PasswordEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PasswordDetailUiState(
    val id: String? = null,
    val title: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val note: String = "",
    val loading: Boolean = true,
    val done: Boolean = false
)

@HiltViewModel
class PasswordDetailViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PasswordDetailUiState())
    val state: StateFlow<PasswordDetailUiState> = _state.asStateFlow()

    fun load(id: String?) {
        if (id == null) { _state.value = _state.value.copy(loading = false); return }
        viewModelScope.launch {
            val e = repository.getById(id)
            _state.value = if (e != null) _state.value.copy(
                id = e.id, title = e.title, username = e.username, password = e.password,
                url = e.url.orEmpty(), note = e.note.orEmpty(), loading = false
            ) else _state.value.copy(loading = false)
        }
    }

    fun onTitle(v: String) { _state.value = _state.value.copy(title = v) }
    fun onUsername(v: String) { _state.value = _state.value.copy(username = v) }
    fun onPassword(v: String) { _state.value = _state.value.copy(password = v) }
    fun onUrl(v: String) { _state.value = _state.value.copy(url = v) }
    fun onNote(v: String) { _state.value = _state.value.copy(note = v) }

    fun save() {
        val s = _state.value
        if (s.title.isBlank() && s.username.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entry = PasswordEntry(
                id = s.id ?: UUID.randomUUID().toString(),
                title = s.title.trim(),
                username = s.username.trim(),
                password = s.password,
                url = s.url.trim().ifBlank { null },
                note = s.note.trim().ifBlank { null },
                updatedAt = now,
                createdAt = now
            )
            if (s.id == null) repository.add(entry) else repository.update(entry)
            _state.value = _state.value.copy(done = true)
        }
    }

    fun delete() {
        val s = _state.value
        if (s.id == null) { _state.value = _state.value.copy(done = true); return }
        viewModelScope.launch {
            repository.delete(s.id)
            _state.value = _state.value.copy(done = true)
        }
    }
}
