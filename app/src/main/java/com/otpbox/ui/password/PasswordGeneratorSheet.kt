package com.otpbox.ui.password

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.otpbox.domain.PasswordGenerator
import com.otpbox.security.ClipboardHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorSheet(
    onDismiss: () -> Unit,
    onUse: (String) -> Unit
) {
    val context = LocalContext.current
    var length by remember { mutableIntStateOf(16) }
    var useUpper by remember { mutableStateOf(true) }
    var useLower by remember { mutableStateOf(true) }
    var useDigits by remember { mutableStateOf(true) }
    var useSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun generate() {
        error = null
        result = null
        when (val r = PasswordGenerator.generate(
            PasswordGenerator.Config(
                length = length,
                useUpper = useUpper,
                useLower = useLower,
                useDigits = useDigits,
                useSymbols = useSymbols,
                excludeAmbiguous = excludeAmbiguous
            )
        )) {
            is PasswordGenerator.Result.Success -> result = r.password
            is PasswordGenerator.Result.Error -> error = r.message
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "生成密码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Text("长度: $length", modifier = Modifier.fillMaxWidth())
            Slider(
                value = length.toFloat(),
                onValueChange = { length = it.toInt() },
                valueRange = 8f..64f,
                steps = 55,
                modifier = Modifier.fillMaxWidth()
            )

            ToggleRow("大写字母 (A-Z)", useUpper) { useUpper = it }
            ToggleRow("小写字母 (a-z)", useLower) { useLower = it }
            ToggleRow("数字 (0-9)", useDigits) { useDigits = it }
            ToggleRow("符号 (!@#$)", useSymbols) { useSymbols = it }
            ToggleRow("排除易混淆字符", excludeAmbiguous) { excludeAmbiguous = it }

            Button(onClick = { generate() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(" 生成")
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            result?.let { pw ->
                OutlinedTextField(
                    value = pw,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("生成的密码") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { ClipboardHelper.copyPlain(context, "password", pw) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text(" 复制")
                    }
                    Button(
                        onClick = { onUse(pw) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("使用")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
