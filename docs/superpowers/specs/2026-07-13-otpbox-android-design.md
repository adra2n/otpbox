# OTPBox — Android OTP Authenticator (Design Spec)

- **Date**: 2026-07-13
- **App name**: OTPBox
- **Package**: `com.otpbox`
- **Status**: Approved design (pending user spec review)

## 1. Goal

A privacy-first Android TOTP authenticator that supports adding accounts via QR scanning and multiple import paths, stores secrets encrypted at rest, optionally syncs encrypted backups to a GitHub Gist, and never exposes secrets to the OS or the network in plaintext.

### Success criteria

- Add an account by any of 6 methods (scan, image, paste URI, manual, JSON import, GA migration).
- Secrets are encrypted at rest; the app locks behind biometric/PIN; screenshots are blocked.
- A user can push an encrypted backup to a GitHub Gist and pull/merge it on another device using a backup password.
- TOTP codes are correct (validated against RFC 6238 test vectors) and refresh with a visible countdown.

### Out of scope (YAGNI)

- HOTP, Steam Guard, OCRA. (TOTP only.)
- Cloud sync beyond GitHub Gist (no Firebase, no self-hosted server).
- Auto sync on every change (manual push/pull only).
- iOS, desktop, web.
- Browser autofill extension.

## 2. Non-goals / constraints

- Minimum SDK 26 (Android 8.0) — required for good Keystore + BiometricPrompt support.
- Target SDK 34. Build with AGP 8, Kotlin 2.0, Jetpack Compose (BOM latest stable).
- No analytics, no telemetry, no third-party crash reporting in v1.
- Single-module Gradle app (`:app`).

## 3. Architecture

Single-module app, MVVM, layered:

```
app/
├── data/
│   ├── local/        Room DAO + entities, SQLCipher open helper
│   ├── crypto/       Keystore key manager, master-key wrap, backup encryption
│   ├── repo/         OtpRepository (single source of truth)
│   ├── backup/       JSON import/export (own schema + Aegis import)
│   └── sync/         GitHub Gist sync (GitHubApi, SyncManager, SyncMerger)
├── domain/
│   ├── model/        OtpEntry, OtpConfig, AddResult, SyncResult
│   ├── otp/          TotpGenerator (RFC 6238)
│   └── parse/        otpauth URI parser, GA migration protobuf parser
├── ui/
│   ├── theme/  nav/  home/  add/  scan/  detail/  settings/  lock/
├── security/         BiometricPrompt manager, AppLockGate, FLAG_SECURE
└── di/               Hilt modules
```

**Data flow**: `UI (Compose) -> ViewModel (StateFlow) -> OtpRepository -> Room(SQLCipher)`. OTP codes are computed on demand from the stored config and are never persisted.

### Key libraries

- Jetpack Compose, Navigation-Compose, Lifecycle-ViewModel, Hilt
- Room + `net.zetetic:android-database-sqlcipher` via `SupportFactory`
- CameraX (core/camera2/view) + ML Kit `barcode-scanning`
- `androidx.biometric:biometric`
- DataStore Preferences (settings: app-lock, sort order, last-sync)
- Retrofit + OkHttp + kotlinx.serialization-json (GitHub API + backup JSON)
- `com.google.zxing:core` (GA migration protobuf decode only; QR capture uses ML Kit)

## 4. Data model

Room entity inside the SQLCipher-encrypted database:

```kotlin
@Entity(tableName = "otp_entries")
data class OtpEntryEntity(
    @PrimaryKey val id: String,        // UUID, stable across devices (sync key)
    val issuer: String,                 // e.g. "GitHub"
    val account: String,                // e.g. "me@example.com"
    val secret: String,                 // Base32 secret
    val algorithm: String,              // "SHA1" | "SHA256" | "SHA512"
    val digits: Int,                    // 6 | 8
    val period: Int,                    // seconds, default 30
    val type: String,                   // "TOTP"
    val color: Int?,                    // optional color tag (ARGB)
    val note: String?,                  // optional note
    val icon: String?,                  // optional icon key / emoji
    val sortOrder: Int,                 // manual sort position
    val deleted: Boolean = false,       // tombstone for sync
    val updatedAt: Long,                // epoch millis, sync tiebreaker
    val createdAt: Long,
)
```

- `deleted` + `updatedAt` enable merge sync without resurrecting deleted entries. A periodic cleanup purges tombstones older than 30 days.
- OTP codes are never stored; always computed from `secret/algorithm/digits/period`.

## 5. Add / scan / import flows

All add paths funnel into a single `OtpRepository.add(entry)`; they differ only in how the `OtpConfig` is obtained.

```
        ┌─ Scan QR (CameraX + ML Kit) ──────┐
        ├─ Pick image -> ML Kit decode ─────┤
Source ─┼─ Paste otpauth URI ───────────────┼─> OtpConfig ─> repo.add() ─> Room
        ├─ Manual entry form ───────────────┤
        ├─ Import JSON backup ──────────────┤
        └─ GA migration QR ─────────────────┘
```

1. **Scan QR (live)** — `ScanScreen` uses CameraX preview + ML Kit `BarcodeScanner`. On a `FORMAT_QR_CODE` result, pass `rawValue` to `OtpParser`. Continuous scanning with duplicate debounce.
2. **Pick image** — `PickVisualMedia` -> `BarcodeScanner.process(InputImage.fromBitmap(...))`. Same parse path as live scan.
3. **Paste otpauth URI** — text field -> `OtpAuthUriParser.parse("otpauth://totp/GitHub:me@x.com?secret=...&issuer=GitHub&algorithm=SHA1&digits=6&period=30")` -> `OtpConfig`.
4. **Manual entry** — form with issuer, account, secret (Base32, validated), algorithm dropdown, digits, period. Auto-generates an otpauth URI preview.
5. **Import JSON backup** — file picker -> JSON -> supports two schemas:
   - **Own schema**: `{ "version": 1, "entries": [ {id, issuer, account, secret, ...} ] }` (plain or password-encrypted via Section 7 format).
   - **Aegis-compatible** (unencrypted export, or Aegis password-encrypted scrypt format): map `entry.issuer/info/secret/algo/digits/period`.
   If encrypted, prompt for password.
6. **GA migration QR** — `otpauth-migration://offline?data=<base64 protobuf>`. Parse protobuf (`MigrationPayload`) into one entry per `OtpParameters`. Source can be live scan or a pasted URI.

### Validation & errors

- Invalid Base32 secret, unknown algorithm, missing issuer -> `AddResult.Error(reason)` shown in a snackbar; no partial entry is saved.
- Duplicate detection (same issuer + account + secret) warns but allows (user may have legitimately re-added).

## 6. OTP code generation

- `TotpGenerator` implements RFC 6238: `HMAC(algorithm, secret, floor(now/period))` truncated to `digits`.
- Secret decoded from Base32 (RFC 4648; tolerate lowercase and missing padding).
- Validated against RFC 6238 Appendix B test vectors (SHA1, secret = "12345678901234567890").
- VM exposes a `flow` emitting current code + remaining seconds, driven by a 1-second ticker. Recomposition is scoped per entry (keyed by entry id) so the countdown ring and code text update without invalidating the whole list.

## 7. Security

### Encrypted storage (SQLCipher)

- On first launch, generate a random 256-bit **master key**, encrypt it with an AES-GCM key from `AndroidKeystore`, store the wrapped master key in `EncryptedSharedPreferences`.
- Room opens via SQLCipher `SupportFactory` using the unwrapped master key as the passphrase. The Keystore key never leaves secure hardware; the DB file on disk is ciphertext.
- `OtpRepository` hides all crypto behind a plain interface so the UI is crypto-agnostic.

### App lock (biometric / PIN)

- `AppLockGate`: on app foreground, if app-lock is enabled, show `BiometricPrompt` (fingerprint/face) with device-credential (PIN/password) fallback.
- Locked state is in-memory. DB is technically decryptable, but the UI refuses to show data until unlocked. Secret-at-rest is the SQLCipher file itself when the app is backgrounded/killed.
- App-lock setting stored in DataStore; toggling requires a successful biometric.

### No screenshots

- `FLAG_SECURE` set on all windows via an Application-level window callback. Blocks screenshots, screen recording, and the task-switcher preview content.

### Encrypted export / backup

- `BackupEncryptor.encrypt(json, password)`:
  - Derive a 256-bit key via **Argon2id** (fall back to PBKDF2-HMAC-SHA256, 600,000 iterations, if Argon2 is unavailable on device).
  - AES-GCM encrypt.
  - Output: `{ "format": "otpbox", "version": 1, "kdf": { "algo": "argon2id"|"pbkdf2", ...params }, "salt": ..., "nonce": ..., "ciphertext": ..., "tag": ... }`.
- Same format is used for both "Export to file" and GitHub sync content. The Gist only ever sees ciphertext.
- **Backup password is separate from the app-lock PIN.** The user sets it once in settings; it is required on any new device for restore/pull. This is intentional: a leaked Gist must not be decryptable with device-local state.

### GitHub PAT handling

- PAT stored in `EncryptedSharedPreferences`; never logged.
- OkHttp interceptor redacts the `Authorization` header from logs.
- Requests only go to `api.github.com`.

## 8. GitHub Gist sync

Manual push/pull; merge by entry ID.

### Module (`data/sync/`)

- `GitHubApi` (Retrofit + OkHttp) — endpoints: get gist, create gist, update gist.
- `SyncManager` — orchestrates push/pull; called only manually from settings.
- `SyncMerger` — union by entry id; `updatedAt` newest wins; deletes honored via the `deleted` tombstone flag.

### Data flow

- **Push**: `OtpRepository.getAll() -> BackupEncryptor.encrypt(password) -> update (or create) Gist`. Store the gist id after first create.
- **Pull**: `get Gist -> BackupEncryptor.decrypt(password) -> SyncMerger.merge(local, remote) -> Room upsert`.
- **Merge rule**: for each id present on either side, take the entry with the greater `updatedAt`. A tombstoned (`deleted=true`) entry with a newer `updatedAt` than a live one wins (stays deleted). Union of all ids is the resulting set.

### UX (settings screen)

- PAT input (password field), gist id (auto-created on first push if blank), **Push** / **Pull** buttons, last-sync timestamp, status/error messages.
- First-time setup prompts for the backup password (set once; shared by export and sync).

### Errors

- Network failure / 401 (bad PAT) / 404 (gist gone) -> `SyncResult.Error(reason)` shown inline; local data untouched.
- Wrong backup password on pull -> decryption fails -> `SyncResult.Error("Wrong backup password")`; local data untouched.

## 9. UI screens (Material 3, Compose)

| Screen | Purpose |
|---|---|
| `LockScreen` | Biometric/PIN gate shown on foreground when app-lock is on. |
| `HomeScreen` | List of entries: issuer avatar (color + initials), account, live code, circular countdown ring synced to period. Tap code = copy + snackbar. Search bar (filters issuer/account). Sort menu (issuer A-Z, custom, recently used). FAB opens add menu (Scan QR / Manual / Paste URI / Import file / Pick image). |
| `ScanScreen` | Full-screen CameraX preview with reticle, live ML Kit decoding; on success haptic + auto-return to Home with the new entry highlighted. |
| `AddScreen` | Tabs: Manual / Paste URI. Manual = form fields + live otpauth preview. Paste = text field + parse button. Duplicate warning inline. |
| `DetailScreen` | View + edit issuer/account/note/color/icon; delete (sets tombstone). Shows current code + countdown. |
| `SettingsScreen` | Sections: App lock (toggle + biometric enroll), Backup (export to file, set backup password), GitHub sync (PAT, gist id, Push/Pull, last-sync, status), About. |

## 10. Testing

- **Unit**: `TotpGenerator` vs RFC 6238 vectors; `OtpAuthUriParser` (valid/invalid/malformed); GA migration protobuf parse; `SyncMerger` (union, conflict, tombstone-resurrection guard); `BackupEncryptor` round-trip + wrong-password failure; Base32 decode edge cases.
- **Instrumented** (androidTest): Room DAO CRUD with SQLCipher open helper on device; `OtpRepository` add/list/delete/tombstone; `AppLockGate` flag behavior.
- **Manual**: live QR scan against real services (GitHub, Google); GA migration QR from Google Authenticator export; Gist push from device A, pull on device B, verify merge.

## 11. Build & permissions

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.INTERNET" />   <!-- GitHub sync only -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

- Camera permission requested on first scan; app is usable without a camera (manual/URI/import paths still work).
- Internet permission is only needed for GitHub sync; the app is fully functional offline.

## 12. Open questions

None remaining after design approval.
