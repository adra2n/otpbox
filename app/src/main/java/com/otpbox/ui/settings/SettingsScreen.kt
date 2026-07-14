package com.otpbox.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.otpbox.BuildConfig
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    var exportContent by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val content = exportContent
        exportContent = null
        if (uri != null && content != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)
                    ?.use { it.write(content.toByteArray()) }
            }.isSuccess
            viewModel.exportConsumed()
            if (!ok) viewModel.setMessage("导出失败：文件无法写入")
        } else {
            viewModel.exportConsumed()
        }
    }

    LaunchedEffect(state.pendingExport) {
        if (state.pendingExport != null) {
            exportContent = state.pendingExport
            exportLauncher.launch("otpbox-backup.json")
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val title = if (selectedCategory == null) "设置" else selectedCategory!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) selectedCategory = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when (selectedCategory) {
            null -> SettingsCategoryList(
                modifier = Modifier.padding(padding),
                onSelect = { selectedCategory = it }
            )
            "安全" -> SecuritySettings(
                modifier = Modifier.padding(padding),
                state = state,
                viewModel = viewModel,
                onSetPin = { showPinDialog = true }
            )
            "备份与同步" -> BackupSyncSettings(
                modifier = Modifier.padding(padding),
                state = state,
                viewModel = viewModel
            )
            "关于" -> AboutSettings(
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                viewModel.setPin(pin)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun SettingsCategoryList(
    modifier: Modifier,
    onSelect: (String) -> Unit
) {
    val categories = listOf(
        Triple("安全", "应用锁 · 自动锁定 · PIN", Icons.Default.Lock),
        Triple("备份与同步", "备份密码 · 导出 · GitHub Gist", Icons.Default.CloudUpload),
        Triple("关于", "版本信息", Icons.Default.Info)
    )
    Column(modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        categories.forEach { (name, desc, icon) ->
            ListItem(
                headlineContent = { Text(name) },
                supportingContent = { Text(desc) },
                leadingContent = { Icon(icon, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.clickable { onSelect(name) }
            )
        }
    }
}

@Composable
private fun SecuritySettings(
    modifier: Modifier,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onSetPin: () -> Unit
) {
    Column(
        modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("安全")
        SwitchRow("应用锁（指纹 / 面容 / PIN）", state.appLockEnabled, viewModel::setAppLock)
        SwitchRow("阻止截屏与录屏", state.secureScreen, viewModel::setSecureScreen)

        if (state.appLockEnabled) {
            Text("自动锁定", style = MaterialTheme.typography.labelLarge)
            val options = listOf(0 to "立即", 60 to "1 分钟后", 300 to "5 分钟后")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (secs, label) ->
                    FilterChip(
                        selected = state.autoLockSeconds == secs,
                        onClick = { viewModel.setAutoLockSeconds(secs) },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PIN 码备用解锁")
                    Text(
                        if (state.pinSet) "已设置 6 位 PIN 码" else "未设置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (state.pinSet) {
                    TextButton(onClick = viewModel::clearPin) { Text("清除") }
                } else {
                    TextButton(onClick = onSetPin) { Text("设置") }
                }
            }
        }
    }
}

@Composable
private fun BackupSyncSettings(
    modifier: Modifier,
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    var backupPw by remember { mutableStateOf("") }
    var githubToken by remember { mutableStateOf("") }
    var gistId by remember { mutableStateOf(state.gistId) }

    Column(
        modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("备份密码")
        Text(
            if (state.hasBackupPassword) "已设置备份密码。"
            else "设置一个密码用于加密导出和云同步。",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = backupPw,
            onValueChange = { backupPw = it },
            label = { Text("备份密码") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.setBackupPassword(backupPw); backupPw = "" },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存备份密码") }

        SectionTitle("导出")
        OutlinedButton(onClick = viewModel::requestExport, modifier = Modifier.fillMaxWidth()) {
            Text("导出加密备份")
        }

        SectionTitle("GitHub Gist 同步")
        OutlinedTextField(
            value = githubToken,
            onValueChange = { githubToken = it },
            label = { Text("GitHub Token（gist 权限）") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.setGithubToken(githubToken); githubToken = "" },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存 Token") }
        Text(
            if (state.hasGithubToken) "Token 已设置。" else "尚未设置 Token。",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = gistId,
            onValueChange = { gistId = it },
            label = { Text("Gist ID（留空则首次推送时自动创建）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { viewModel.setGistId(gistId) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存 Gist ID") }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = viewModel::push,
                enabled = !state.syncing,
                modifier = Modifier.weight(1f)
            ) { Text("推送") }
            Button(
                onClick = viewModel::pull,
                enabled = !state.syncing,
                modifier = Modifier.weight(1f)
            ) { Text("拉取") }
        }
        if (state.syncing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                Text("同步中…")
            }
        }
        if (state.lastSyncAt > 0) {
            Text(
                "上次同步：${java.text.DateFormat.getDateTimeInstance().format(state.lastSyncAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun AboutSettings(modifier: Modifier) {
    Column(
        modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle("关于")
        Text(
            "口令盒子 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "本地加密存储的 TOTP 两步验证与验证码管理工具。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && pin != confirm
    val valid = pin.length == 6 && pin == confirm

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置 6 位 PIN 码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("输入 PIN 码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it },
                    label = { Text("再次输入") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = mismatch
                )
                if (mismatch) {
                    Text("两次输入不一致", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = valid) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
