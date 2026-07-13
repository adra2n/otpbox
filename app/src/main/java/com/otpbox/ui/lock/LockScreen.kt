package com.otpbox.ui.lock

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otpbox.ui.theme.WarnAmber

private const val PIN_LENGTH = 6

@Composable
fun LockScreen(
    pinEnabled: Boolean,
    biometricEnabled: Boolean,
    onBiometricClick: () -> Unit,
    onPinEntered: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.8f))
        Icon(
            Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(12.dp))
        Text("口令盒子", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(4.dp))
        Text(
            when {
                error -> "PIN 码错误，请重试"
                pinEnabled -> "输入 PIN 码解锁"
                else -> "验证身份以解锁"
            },
            color = if (error) WarnAmber else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.size(28.dp))

        if (pinEnabled) {
            PinDots(filled = pin.length, error = error)
            Spacer(Modifier.weight(1f))
            Keypad(
                onDigit = { d ->
                    if (pin.length < PIN_LENGTH) {
                        error = false
                        pin += d
                        if (pin.length == PIN_LENGTH) {
                            val ok = onPinEntered(pin)
                            if (!ok) {
                                error = true
                                pin = ""
                            }
                        }
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                onBiometric = if (biometricEnabled) onBiometricClick else null
            )
        } else {
            Spacer(Modifier.size(24.dp))
            BiometricButton(onBiometricClick)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun PinDots(filled: Int, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PIN_LENGTH) { i ->
            val active = i < filled
            val color = when {
                error -> WarnAmber
                active -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val dotSize by animateDpAsState(if (active) 16.dp else 12.dp, label = "dot")
            Box(
                modifier = Modifier
                    .size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(dotSize).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { d -> KeyButton(d) { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (onBiometric != null) {
                IconKey(Icons.Default.Fingerprint, "指纹", onBiometric)
            } else {
                Spacer(Modifier.size(72.dp))
            }
            KeyButton("0") { onDigit("0") }
            IconKey(Icons.Default.Backspace, "删除", onBackspace)
        }
    }
}

@Composable
private fun KeyButton(digit: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(digit, fontSize = 28.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IconKey(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BiometricButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = "指纹解锁",
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.size(8.dp))
        Text("点击使用指纹 / 面容解锁", style = MaterialTheme.typography.bodySmall)
    }
}
