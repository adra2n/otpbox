# 口令盒子 (OTPBox)

一款注重隐私与安全的 Android 两步验证（2FA / TOTP）认证器应用。所有密钥均在本地加密存储，支持指纹 / PIN 应用锁，并可选地通过你自己的 GitHub Gist 进行端到端加密同步。

> 基于 RFC 6238 (TOTP) 实现，兼容 Google Authenticator、Aegis 等主流认证器的导入与迁移。

## 功能特性

### 账号管理
- **扫码添加**：使用 CameraX + ML Kit 实时扫描 `otpauth://` 二维码
- **图片导入**：从相册选择含二维码的图片自动识别
- **手动录入**：填写密钥（Base32）或粘贴 `otpauth://` 链接
- **JSON 导入**：支持口令盒子自有备份格式与 **Aegis 未加密导出**
- **Google Authenticator 迁移**：解析 `otpauth-migration://` 批量导入
- 编辑服务名 / 账号 / 备注，删除账号
- 首页搜索、排序（自定义 / 按服务名 / 按账号）

### 界面
- Material 3 现代卡片式设计 + 品牌配色，支持深色模式
- 彩色首字母头像、等宽大字验证码、一键复制
- 顶部全局倒计时进度条（剩余 ≤5 秒变色提示）

### 安全
- **SQLCipher** 加密数据库，密钥由 Android Keystore（硬件级，若支持）封装
- 敏感配置存于 **EncryptedSharedPreferences**
- **应用锁**：指纹 / 面容 / 设备凭证解锁
- **6 位 PIN 码**备用解锁（PBKDF2 加盐哈希）
- **自动锁定**：立即 / 1 分钟 / 5 分钟
- 默认开启 `FLAG_SECURE`，阻止截屏与录屏
- 关闭系统自动备份，防止密钥外泄

### 备份与同步
- **加密导出**：PBKDF2-HMAC-SHA256 (600k) 派生密钥 + AES-256-GCM 信封
- **GitHub Gist 同步**：手动推送 / 拉取，云端仅存密文
- 按账号 ID 合并（`updatedAt` 最新优先，尊重删除墓碑），多设备安全合并
- 备份密码与应用锁 PIN 相互独立

## 技术栈

| 领域 | 技术 |
|------|------|
| 语言 / UI | Kotlin · Jetpack Compose · Material 3 |
| 架构 | MVVM · Hilt 依赖注入 |
| 存储 | Room + SQLCipher · DataStore · EncryptedSharedPreferences |
| 相机 / 识别 | CameraX · ML Kit Barcode Scanning |
| 网络 | Retrofit · OkHttp · kotlinx.serialization |
| 加密 | Android Keystore · AES-256-GCM · PBKDF2 |

- **minSdk** 26 · **target/compileSdk** 34
- **Gradle** 8.9 · **AGP** 8.7.3 · **Kotlin** 2.0.21 · **JDK** 21

## 构建

```bash
# 配置 Android SDK 路径（或在 local.properties 中设置 sdk.dir）
export ANDROID_HOME=/path/to/android-sdk

# 编译 Debug APK
./gradlew :app:assembleDebug

# 运行单元测试
./gradlew :app:testDebugUnitTest
```

产物位于 `app/build/outputs/apk/debug/`。

## GitHub 同步配置

1. 在 GitHub 生成一个具有 **gist** 权限的 Personal Access Token
2. 打开 App → 设置 → GitHub Gist 同步，填入 Token
3. 设置备份密码（用于加密同步内容）
4. 首次「推送」会自动创建私有 Gist，之后可在其他设备填入相同的 Gist ID 与备份密码「拉取」

> 云端 Gist 中仅保存加密后的密文，你的 Token 与备份密码不会上传。

## 项目结构

```
app/src/main/java/com/otpbox/
├── domain/          # OTP 核心（Base32、TOTP、URI/迁移解析）
├── data/
│   ├── local/       # Room + SQLCipher
│   ├── crypto/      # Keystore 密钥、加密存储
│   ├── repo/        # 仓库
│   ├── backup/      # 加密备份与 JSON 导入
│   ├── settings/    # DataStore 设置
│   └── sync/        # GitHub Gist 同步与合并
├── security/        # 生物识别、PIN
├── di/              # Hilt 模块
└── ui/              # Compose 界面（home/scan/add/import/detail/settings/lock）
```

## 安全说明

- 本应用仅支持 **TOTP**（基于时间的一次性密码）。
- 所有密钥离线加密保存，卸载应用即清除；请务必先做好加密备份。
- 备份密码一旦遗忘将无法恢复加密数据，请妥善保管。

## 赞赏

如果这个应用对你有帮助，欢迎扫码赞赏支持开发：

![赞赏码](赞赏码.jpg)

## 许可证

见 [LICENSE](LICENSE)。
