package com.otpbox

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.otpbox.data.settings.PrivacyStore
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.security.BiometricAuthenticator
import com.otpbox.security.PinManager
import com.otpbox.ui.PrivacyConsentScreen
import com.otpbox.ui.lock.LockScreen
import com.otpbox.ui.nav.OtpNavHost
import com.otpbox.ui.theme.OTPBoxTheme
import com.otpbox.util.UmengInit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var pinManager: PinManager

    private var appLockEnabled = false
    private var autoLockSeconds = 0
    private var backgroundedAt = 0L
    private var isPrompting = false
    private val lockedState = mutableStateOf(false)
    private val privacyAgreed = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            settingsRepository.appLockEnabled.collect { appLockEnabled = it }
        }
        lifecycleScope.launch {
            settingsRepository.autoLockSeconds.collect { autoLockSeconds = it }
        }
        lifecycleScope.launch {
            settingsRepository.secureScreen.collect { secure ->
                if (secure) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
        lifecycleScope.launch {
            if (settingsRepository.appLockEnabled.first()) {
                lockedState.value = true
            }
        }
        // 隐私同意状态需在 Context attach 后读取（构造期 Context 尚未就绪）
        lifecycleScope.launch {
            privacyAgreed.value = PrivacyStore(this@MainActivity).isAgreed()
        }

        setContent {
            OTPBoxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (privacyAgreed.value) {
                        OtpNavHost()
                    } else {
                        PrivacyConsentScreen(
                            onAgree = {
                                lifecycleScope.launch {
                                    PrivacyStore(this@MainActivity).setAgreed()
                                    UmengInit.init(this@MainActivity)
                                    privacyAgreed.value = true
                                }
                            },
                            onDecline = { finish() }
                        )
                    }
                    if (lockedState.value) {
                        LockScreen(
                            pinEnabled = pinManager.isPinSet,
                            biometricEnabled = BiometricAuthenticator.canAuthenticate(this),
                            onBiometricClick = { promptBiometric() },
                            onPinEntered = { pin ->
                                pinManager.verify(pin).also { if (it) lockedState.value = false }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (appLockEnabled) {
            backgroundedAt = SystemClock.elapsedRealtime()
            lockedState.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!appLockEnabled) return
        if (backgroundedAt > 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - backgroundedAt) / 1000
            if (elapsed >= autoLockSeconds) lockedState.value = true
        }
        if (lockedState.value) maybeAutoPromptBiometric()
    }

    private fun maybeAutoPromptBiometric() {
        if (BiometricAuthenticator.canAuthenticate(this)) promptBiometric()
    }

    private fun promptBiometric() {
        if (isPrompting) return
        if (!BiometricAuthenticator.canAuthenticate(this)) {
            if (!pinManager.isPinSet) lockedState.value = false
            return
        }
        isPrompting = true
        BiometricAuthenticator.authenticate(
            activity = this,
            title = "解锁口令盒子",
            subtitle = "验证身份以查看验证码",
            onSuccess = {
                lockedState.value = false
                isPrompting = false
            },
            onError = { isPrompting = false }
        )
    }
}
