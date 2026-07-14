package com.otpbox.data.backup

import kotlinx.serialization.Serializable

import com.otpbox.domain.model.PasswordEntry

/** Plaintext backup content (what gets encrypted). */
@Serializable
data class BackupContent(
    val version: Int = 2,
    val entries: List<com.otpbox.domain.model.OtpEntry> = emptyList(),
    val passwords: List<com.otpbox.domain.model.PasswordEntry> = emptyList()
)

/** Encrypted backup envelope (what is written to file / GitHub gist). */
@Serializable
data class EncryptedBackup(
    val format: String = "otpbox",
    val version: Int = 1,
    val kdf: KdfParams,
    val salt: String,
    val nonce: String,
    val ciphertext: String
)

@Serializable
data class KdfParams(
    val algo: String = "pbkdf2-hmac-sha256",
    val iterations: Int = 600_000,
    val keyBits: Int = 256
)
