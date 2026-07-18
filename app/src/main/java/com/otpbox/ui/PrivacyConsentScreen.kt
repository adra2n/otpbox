package com.otpbox.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otpbox.ui.theme.BrandPrimary

@Composable
fun PrivacyConsentScreen(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "隐私政策与用户协议",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "欢迎使用口令盒子。我们非常重视您的个人信息和隐私保护。在继续使用前，请阅读并同意以下说明：",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "· 我们仅收集用于改进产品体验的匿名统计数据（如启动次数、页面访问），不包含您的任何验证码或账户信息。\n" +
                        "· 统计数据由第三方分析服务（友盟）处理，您可随时在系统设置中清除应用数据以停止收集。\n" +
                        "· 您的全部令牌与验证码仅存储于本机设备数据库（SQLCipher 加密），不会上传至任何服务器。",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "点击「同意」，即表示您理解并同意上述隐私处理方式。",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onAgree,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("同意并继续", fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("不同意", fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "若选择「不同意」，应用将无法启动。",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
