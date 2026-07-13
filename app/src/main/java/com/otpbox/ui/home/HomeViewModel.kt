package com.otpbox.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.data.settings.SortOrder
import com.otpbox.domain.model.OtpCode
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeItem(val entry: OtpEntry, val code: OtpCode)

data class HomeUiState(
    val items: List<HomeItem> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.CUSTOM,
    val loading: Boolean = true,
    val globalProgress: Float = 1f,
    val globalRemaining: Int = 30,
    val periodSeconds: Int = 30
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OtpRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000)
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(
            repository.observeEntries(),
            query,
            settings.sortOrder,
            ticker
        ) { entries, q, sort, now ->
            val filtered = entries.filter { !it.deleted }
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
            val refPeriod = sorted.groupingBy { it.period }.eachCount()
                .maxByOrNull { it.value }?.key ?: 30
            val secs = now / 1000L
            val remaining = (refPeriod - (secs % refPeriod)).toInt()
            HomeUiState(
                items = sorted.map { entry ->
                    HomeItem(entry, TotpGenerator.codeFor(entry, now))
                },
                query = q,
                sortOrder = sort,
                loading = false,
                globalProgress = remaining.toFloat() / refPeriod.toFloat(),
                globalRemaining = remaining,
                periodSeconds = refPeriod
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

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
