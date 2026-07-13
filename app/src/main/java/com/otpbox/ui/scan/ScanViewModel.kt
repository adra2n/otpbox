package com.otpbox.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.domain.parse.OtpParser
import com.otpbox.data.repo.OtpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class ScanUiState(
    val message: String? = null,
    val savedCount: Int = 0,
    val done: Boolean = false
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: OtpRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val handled = AtomicBoolean(false)

    fun onQrDetected(raw: String) {
        if (!handled.compareAndSet(false, true)) return
        when (val result = OtpParser.parse(raw)) {
            is OtpParser.Result.Success -> viewModelScope.launch {
                repository.addAll(result.entries)
                _state.value = ScanUiState(
                    message = "Added ${result.entries.size} account(s)",
                    savedCount = result.entries.size,
                    done = true
                )
            }
            is OtpParser.Result.Error -> {
                _state.value = ScanUiState(message = result.message)
                handled.set(false)
            }
        }
    }
}
