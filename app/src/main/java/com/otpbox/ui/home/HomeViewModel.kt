package com.otpbox.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.data.settings.SortOrder
import com.otpbox.domain.model.OtpEntry
import com.otpbox.domain.otp.TotpGenerator
import com.otpbox.data.repo.OtpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeItem(
    val entry: OtpEntry,
    val code: String,
    val remainingSeconds: Int,
    val progress: Float
)

data class HomeUiState(
    val items: List<HomeItem> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.CUSTOM,
    val loading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OtpRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val entries = repository.observeEntries()

    /**
     * Stable screen state (list + sort + query). Does NOT tick every second, so the
     * chrome (top bar, search field, scaffold) only recomposes when the entry set,
     * sort order, or query actually changes.
     */
    val uiState: StateFlow<HomeUiState> =
        combine(entries, query, settings.sortOrder) { list, q, sort ->
            val filtered = list.filter { !it.deleted }
                .filter { e ->
                    q.isBlank() ||
                        e.issuer.contains(q, ignoreCase = true) ||
                        e.account.contains(q, ignoreCase = true)
                }
            val sorted = when (sort) {
                SortOrder.CUSTOM -> filtered.sortedBy { it.sortOrder }
                SortOrder.ISSUER -> filtered.sortedBy { it.issuer.lowercase() }
                SortOrder.ACCOUNT -> filtered.sortedBy { it.account.lowercase() }
            }
            HomeUiState(
                items = sorted.map { entry ->
                    val c = TotpGenerator.codeFor(entry)
                    HomeItem(entry, c.code, c.remainingSeconds, c.progress)
                },
                query = q,
                sortOrder = sort,
                loading = false
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * Per-entry live codes, ticking every second but only emitting when a code
     * actually changes (typically every entry period, e.g. 30s). This feeds only
     * the list cards, keeping per-second updates off the screen chrome.
     */
    val codes: StateFlow<Map<String, HomeItem>> =
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(1000)
            }
        }.combine(entries) { now, list ->
            list.filter { !it.deleted }.associate { entry ->
                val c = TotpGenerator.codeFor(entry, now)
                entry.id to HomeItem(entry, c.code, c.remainingSeconds, c.progress)
            }
        }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settings.setSortOrder(order) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}
