package com.otpbox

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.data.settings.PrivacyStore
import com.otpbox.security.BiometricAuthenticator
import com.otpbox.security.PinManager
import com.otpbox.ui.lock.LockScreen
import com.otpbox.ui.nav.OtpNavHost
import com.otpbox.ui.theme.OTPBoxTheme
import com.otpbox.util.initUmengIfAllowed
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

        setContent {
            OTPBoxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val privacyAgreed = remember { mutableStateOf(PrivacyStore(this).isAgreedSync()) }
                    if (!privacyAgreed.value) {
                        PrivacyGateScreen(
                            onAgree = {
                                lifecycleScope.launch {
                                    PrivacyStore(this@MainActivity).setAgreed()
                                    initUmengIfAllowed(this@MainActivity)
                                    privacyAgreed.value = true
                                }
                            }
                        )
                    } else {
                        OtpNavHost()
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

@Composable
private fun PrivacyGateScreen(onAgree: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("隐私政策", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "我们重视您的隐私。本应用会收集设备信息与使用统计（友盟 U-App），用于改进产品体验。请在继续使用前阅读并同意隐私政策。",
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Spacer(Modifier.width(4.dp))
            Text("我已阅读并同意《隐私政策》", fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAgree,
            enabled = agreed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("同意并继续")
        }
    }
}
