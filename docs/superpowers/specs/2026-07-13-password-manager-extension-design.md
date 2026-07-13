# 口令盒子密码管理扩展设计

- 日期：2026-07-13
- 状态：已确认设计，待实现
- 关联：`2026-07-13-otpbox-android-design.md`（TOTP 认证器基础设计）

## 背景与目标

现有「口令盒子」是一款 Android TOTP 认证器，已具备 SQLCipher 加密数据库、Android Keystore 封装主密钥、应用锁（指纹/PIN）、加密备份（PBKDF2 600k + AES-256-GCM）、GitHub Gist 同步等安全基础设施。

本次扩展在同一 App 内新增「密码管理」能力，与验证码并列为两个 Tab，最大化复用现有加密、锁定与同步基础设施，形成一体化「口令 + 密码盒子」。

第一版范围（严格遵循 YAGNI）：
- **核心**：密码条目的增删改查、搜索、复制、明文显示切换。
- **密码生成器**：可调长度与字符集。
- **备份与同步**：密码纳入现有加密备份与 Gist 同步。

明确**不做**（本版排除）：浏览器/系统自动填充、弱密码或重复密码检测、分组/标签、自定义字段。

## 架构与导航

- 引入底部导航栏（`NavigationBar`），包含两个 Tab：
  - **验证码**：现有 Home 界面。
  - **密码**：新增密码列表界面。
- 设置入口保持在各 Tab 右上角的图标，进入现有 Settings 界面。
- 密码与验证码复用**同一个 SQLCipher 数据库**（新增 `password_entry` 表），共用 `DbKeyManager` 的同一主密钥。
- 复用现有应用锁：进入密码 Tab 不需要额外二次验证，锁定/自动锁定行为与全局一致。

## 数据模型

新增领域模型 `PasswordEntry`（`domain/model`）：

```
PasswordEntry(
    id: String,             // UUID
    title: String,          // 标题 / 网站名
    username: String,
    password: String,
    url: String? = null,
    note: String? = null,
    sortOrder: Int = 0,
    deleted: Boolean = false,       // 同步用墓碑
    updatedAt: Long,
    createdAt: Long
)
```

- `@Serializable`，供备份/同步序列化。
- 对应 Room 实体 `PasswordEntryEntity`（`data/local`）、`PasswordDao`、并入现有 `OtpDatabase`。
- 新增 `PasswordRepository` / `PasswordRepositoryImpl`，接口暴露 `observeEntries()`、`upsert()`、`delete()`（软删除，置 `deleted=true` 并更新 `updatedAt`）。

## 数据库迁移

- `OtpDatabase` 版本号 +1。
- 提供**非破坏性** `Migration`，`CREATE TABLE password_entry (...)`，保护现有用户 TOTP 数据。
- 迁移在 `AppModule` 构建 Room 时通过 `.addMigrations(...)` 注册；保留 `SupportFactory`（SQLCipher）。

## 密码模块 UI（Jetpack Compose）

- **PasswordListScreen + PasswordListViewModel**
  - 卡片列表：标题、用户名、复制按钮；首字母彩色头像复用 `avatarColorFor`。
  - 顶部搜索（按 title/username 过滤）、FAB「添加」、空状态。
  - 点击卡片进入详情，复制按钮复制密码。
- **PasswordDetailScreen + PasswordDetailViewModel**（查看/编辑合一）
  - 字段：标题、用户名、密码、网址、备注。
  - 密码字段默认隐藏（圆点掩码），提供显示/隐藏切换。
  - 复制用户名、复制密码；保存、删除。
- **PasswordEditScreen**（新增，或与详情共用编辑态）
  - 各字段输入；密码输入框旁提供「生成密码」按钮，打开生成器底部弹窗。

### 密码生成器

- `PasswordGenerator`（`domain`，纯函数、可测试）：
  - 参数：`length`（8–64）、`useUpper`、`useLower`、`useDigits`、`useSymbols`、`excludeAmbiguous`（可选，排除如 `O0l1I` 等易混字符）。
  - 保证：结果长度正确；每个被启用的字符类**至少出现一次**；使用 `SecureRandom`。
  - 至少启用一类字符，否则返回错误/默认启用小写。
- UI：底部弹窗，长度滑块 + 各字符类开关 + 排除易混开关 + 「重新生成」+「使用」。

## 剪贴板安全

- 新增 `ClipboardHelper`（`security` 或 `util`）：
  - 复制敏感内容时，在 Android 13 (API 33) 及以上通过 `ClipDescription` 的 `EXTRA_IS_SENSITIVE` 标记为敏感（不进入剪贴板历史/预览）。
  - 复制后启动一个延迟任务，**30 秒**后清空剪贴板（仅当当前剪贴板内容仍是本次复制的内容时才清除，避免覆盖用户后续复制）。
  - 用于密码复制；用户名复制可选普通复制。

## 备份与同步

- **备份模型升级**（`data/backup/BackupModels`）：
  - 载荷结构：`{ version, entries: [OtpEntry], passwords: [PasswordEntry] }`，`version` 号 +1。
  - `JsonBackupImporter` 解析新格式；对旧备份（无 `passwords` 字段）向后兼容，按空列表处理，TOTP 照常导入。
  - Aegis 导入路径不变（仅 TOTP）。
- **加密导出**：`BackupEncryptor` 逻辑不变（对整体明文 JSON 加密），载荷自然包含密码。
- **同步合并**（`data/sync/SyncMerger`）：
  - 新增对 `passwords` 的合并：按 `id` 取并集，`updatedAt` 最新者胜，尊重 `deleted` 墓碑，与 TOTP 合并逻辑一致。
- **SyncManager**：推送前将 otp + password 一并组装进载荷加密上传；拉取后分别写回两个仓库。

## 错误处理

- 生成器：无字符类启用时给出明确错误或回退。
- 迁移失败：Room 迁移异常向上抛出（不做破坏性 fallback），避免数据丢失。
- 导入：旧格式/缺字段容错；解析失败给出用户可读提示（复用现有导入错误反馈）。
- 剪贴板清除：任务需容忍 App 进入后台/被杀（尽力而为，不保证绝对清除，属可接受降级）。

## 测试

- `PasswordGeneratorTest`：长度正确；启用的每类字符至少各一次；仅单类时不含其他类；排除易混时不含易混字符。
- `SyncMergerTest` 扩展：密码按 id 合并、newest-updatedAt 胜、墓碑生效。
- `JsonBackupImporterTest` 扩展：新格式（含密码）导入；旧格式（无 passwords 字段）向后兼容导入。
- 现有 36 项测试保持通过。

## 组件边界小结

| 组件 | 职责 | 依赖 |
|------|------|------|
| `PasswordEntry` / Entity / Dao | 密码数据定义与持久化 | Room + SQLCipher |
| `PasswordRepository` | 密码读写抽象（含软删） | Dao |
| `PasswordGenerator` | 纯函数生成随机密码 | SecureRandom |
| `ClipboardHelper` | 敏感复制 + 定时清除 | Android Clipboard |
| Password UI（list/detail/edit + VM） | 展示与编辑 | Repository, Generator, ClipboardHelper |
| `BackupModels` / `JsonBackupImporter` | 备份含密码 + 向后兼容 | serialization |
| `SyncMerger` / `SyncManager` | 密码同步合并与传输 | GitHubApi, Repository |
| 底部导航 / NavHost | Tab 切换 | 各屏幕 |

## 交付验收

- 底部两个 Tab 可切换；密码可增删改查、搜索、复制、明文切换。
- 密码生成器可用并可写入编辑表单。
- 复制密码 30 秒后剪贴板清空且标记敏感（API 33+）。
- 加密导出/Gist 同步包含密码；旧备份仍能导入。
- `assembleDebug` 构建成功；全部单元测试通过。
