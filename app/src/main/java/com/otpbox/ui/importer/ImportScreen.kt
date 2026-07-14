package com.otpbox.ui.importer

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()?.let { viewModel.importJson(it) }
                ?: viewModel.setError("Could not read file")
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()?.let { image ->
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val raw = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                        if (raw != null) viewModel.importQrRaw(raw)
                        else viewModel.setError("No QR code found in image")
                    }
                    .addOnFailureListener { viewModel.setError("Failed to read image") }
            } ?: viewModel.setError("Could not open image")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Import accounts from a JSON backup (OTPBox encrypted or unencrypted, " +
                    "or Aegis unencrypted) or from an image containing a QR code.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = { jsonLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import from JSON file")
            }
            Button(
                onClick = { imageLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Import QR from image")
            }

            if (state.needPassword) {
                var password by remember { mutableStateOf("") }
                Text(
                    "该备份文件已加密，请输入备份密码以解密导入。",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("备份密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.importEncrypted(password) },
                    enabled = password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("解密并导入")
                }
                TextButton(onClick = { viewModel.setError("已取消加密备份导入") }) {
                    Text("取消")
                }
            }

            state.message?.let {
                Text(
                    it,
                    color = if (state.isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
