package com.otpbox

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.otpbox.data.settings.SettingsRepository
import com.otpbox.security.BiometricAuthenticator
import com.otpbox.security.PinManager
import com.otpbox.ui.lock.LockScreen
import com.otpbox.ui.nav.OtpNavHost
import com.otpbox.ui.nav.Routes
import com.otpbox.ui.theme.OTPBoxTheme
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
                    if (lockedState.value) {
                        LockScreen(
                            pinEnabled = pinManager.isPinSet,
                            biometricEnabled = BiometricAuthenticator.canAuthenticate(this),
                            onBiometricClick = { promptBiometric() },
                            onPinEntered = { pin ->
                                pinManager.verify(pin).also { if (it) lockedState.value = false }
                            }
                        )
                    } else {
                        UnlockedApp()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (appLockEnabled) backgroundedAt = SystemClock.elapsedRealtime()
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
private fun UnlockedApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = if (currentRoute?.startsWith("password") == true) 1 else 0

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    label = { Text("验证码") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        navController.navigate(Routes.PASSWORDS) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    label = { Text("密码") }
                )
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(innerPadding)) {
            OtpNavHost(navController = navController)
        }
    }
}
