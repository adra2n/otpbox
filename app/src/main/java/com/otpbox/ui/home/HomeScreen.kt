package com.otpbox.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otpbox.data.settings.SortOrder
import com.otpbox.ui.theme.WarnAmber
import com.otpbox.ui.theme.avatarColorFor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddManual: () -> Unit,
    onScan: () -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSearch by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::setQuery,
                            placeholder = { Text("搜索") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("口令盒子", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                    IconButton(onClick = { showSort = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
                    }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        val labels = mapOf(
                            SortOrder.CUSTOM to "自定义",
                            SortOrder.ISSUER to "按服务名",
                            SortOrder.ACCOUNT to "按账号"
                        )
                        SortOrder.values().forEach { order ->
                            DropdownMenuItem(
                                text = { Text(labels[order] ?: order.name) },
                                onClick = {
                                    viewModel.setSortOrder(order)
                                    showSort = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加") }
            )
        }
    ) { padding ->
        if (state.items.isEmpty() && !state.loading) {
            EmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                CountdownBar(
                    progress = state.globalProgress,
                    remaining = state.globalRemaining
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.entry.id }) { item ->
                        OtpCard(
                            item = item,
                            onCopy = {
                                clipboard.setText(AnnotatedString(item.code))
                                scope.launch { snackbar.showSnackbar("验证码已复制") }
                            },
                            onOpenDetail = { onOpenDetail(item.entry.id) }
                        )
                    }
                    item { Spacer(Modifier.size(72.dp)) }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Text(
                "添加账号",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            )
            ListItem(
                headlineContent = { Text("扫描二维码") },
                supportingContent = { Text("对准服务提供的二维码") },
                leadingContent = { Icon(Icons.Default.QrCodeScanner, null) },
                modifier = Modifier.combinedClickable { showSheet = false; onScan() }
            )
            ListItem(
                headlineContent = { Text("手动输入") },
                supportingContent = { Text("填写密钥或粘贴 otpauth 链接") },
                leadingContent = { Icon(Icons.Default.Edit, null) },
                modifier = Modifier.combinedClickable { showSheet = false; onAddManual() }
            )
            ListItem(
                headlineContent = { Text("从文件 / 图片导入") },
                supportingContent = { Text("JSON 备份或含二维码的图片") },
                leadingContent = { Icon(Icons.Default.Upload, null) },
                modifier = Modifier.combinedClickable { showSheet = false; onImport() }
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun CountdownBar(progress: Float, remaining: Int) {
    val color = if (remaining <= 5) WarnAmber else MaterialTheme.colorScheme.primary
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (progress >= 0.99f) snap() else tween(1000, easing = LinearEasing),
        label = "countdown"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${remaining}s",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OtpCard(
    item: HomeItem,
    onCopy: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val entry = item.entry
    val title = entry.issuer.ifBlank { entry.account }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onCopy, onLongClick = onOpenDetail),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (entry.account.isNotBlank() && entry.issuer.isNotBlank()) {
                    Text(
                        text = entry.account,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text = formatCode(item.code),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    val bg = avatarColorFor(name)
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Outlined.Shield,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(16.dp))
        Text("还没有账号", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(6.dp))
        Text(
            "点击右下角「添加」，扫码或手动录入你的两步验证账号",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

private fun formatCode(code: String): String =
    when (code.length) {
        6 -> "${code.substring(0, 3)} ${code.substring(3)}"
        8 -> "${code.substring(0, 4)} ${code.substring(4)}"
        else -> code
    }
