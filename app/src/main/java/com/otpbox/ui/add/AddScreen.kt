package com.otpbox.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onBack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val scroll = rememberScrollState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    clipboard.getText()?.text?.let { viewModel.savePastedUri(it) }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Paste otpauth:// link from clipboard")
            }

            Text("Or enter manually", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = state.issuer,
                onValueChange = viewModel::onIssuer,
                label = { Text("Issuer (e.g. GitHub)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.account,
                onValueChange = viewModel::onAccount,
                label = { Text("Account (e.g. you@example.com)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.secret,
                onValueChange = viewModel::onSecret,
                label = { Text("Secret key (Base32)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Algorithm", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SHA1", "SHA256", "SHA512").forEach { algo ->
                    FilterChip(
                        selected = state.algorithm == algo,
                        onClick = { viewModel.onAlgorithm(algo) },
                        label = { Text(algo) }
                    )
                }
            }

            Text("Digits", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6, 8).forEach { d ->
                    FilterChip(
                        selected = state.digits == d,
                        onClick = { viewModel.onDigits(d) },
                        label = { Text(d.toString()) }
                    )
                }
            }

            Text("Period (seconds)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60).forEach { p ->
                    FilterChip(
                        selected = state.period == p,
                        onClick = { viewModel.onPeriod(p) },
                        label = { Text(p.toString()) }
                    )
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = viewModel::saveManual, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
