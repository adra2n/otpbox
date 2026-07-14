package com.otpbox.ui.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.data.repo.PasswordRepository
import com.otpbox.domain.model.PasswordEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PasswordListUiState(
    val items: List<PasswordEntry> = emptyList(),
    val query: String = ""
)

@HiltViewModel
class PasswordListViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<PasswordListUiState> =
        combine(repository.observeEntries(), _query) { entries, q ->
            val filtered = if (q.isBlank()) entries else entries.filter {
                it.title.contains(q, ignoreCase = true) || it.username.contains(q, ignoreCase = true)
            }
            PasswordListUiState(items = filtered, query = q)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PasswordListUiState())

    fun setQuery(q: String) { _query.value = q }
}
