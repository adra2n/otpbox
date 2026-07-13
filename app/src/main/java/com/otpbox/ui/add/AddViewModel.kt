package com.otpbox.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otpbox.domain.model.OtpEntry
import com.otpbox.domain.otp.Base32
import com.otpbox.domain.parse.OtpParser
import com.otpbox.data.repo.OtpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddUiState(
    val issuer: String = "",
    val account: String = "",
    val secret: String = "",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: OtpRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    fun onIssuer(v: String) { _state.value = _state.value.copy(issuer = v, error = null) }
    fun onAccount(v: String) { _state.value = _state.value.copy(account = v, error = null) }
    fun onSecret(v: String) { _state.value = _state.value.copy(secret = v, error = null) }
    fun onAlgorithm(v: String) { _state.value = _state.value.copy(algorithm = v) }
    fun onDigits(v: Int) { _state.value = _state.value.copy(digits = v) }
    fun onPeriod(v: Int) { _state.value = _state.value.copy(period = v) }

    fun saveManual() {
        val s = _state.value
        val cleanSecret = s.secret.replace(" ", "").uppercase()
        if (s.issuer.isBlank() && s.account.isBlank()) {
            _state.value = s.copy(error = "Enter an issuer or account name")
            return
        }
        if (!Base32.isValid(cleanSecret)) {
            _state.value = s.copy(error = "Secret is not valid Base32")
            return
        }
        viewModelScope.launch {
            repository.add(
                OtpEntry(
                    id = UUID.randomUUID().toString(),
                    issuer = s.issuer.trim(),
                    account = s.account.trim(),
                    secret = cleanSecret,
                    algorithm = s.algorithm,
                    digits = s.digits,
                    period = s.period,
                    type = "TOTP"
                )
            )
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun savePastedUri(text: String) {
        when (val result = OtpParser.parse(text)) {
            is OtpParser.Result.Success -> viewModelScope.launch {
                repository.addAll(result.entries)
                _state.value = _state.value.copy(saved = true)
            }
            is OtpParser.Result.Error ->
                _state.value = _state.value.copy(error = result.message)
        }
    }
}
