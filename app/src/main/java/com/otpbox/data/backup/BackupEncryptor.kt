package com.otpbox.data.backup

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Password-based encryption of backup content using PBKDF2-HMAC-SHA256 for key
 * derivation and AES-256-GCM for authenticated encryption. The output is a
 * self-describing JSON envelope; the same format is used for file export and
 * GitHub gist sync, so the remote store never sees plaintext secrets.
 */
class BackupEncryptor(
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {

    fun encrypt(content: BackupContent, password: String): String {
        require(password.isNotEmpty()) { "Backup password must not be empty" }
        val kdf = KdfParams()
        val salt = randomBytes(16)
        val key = deriveKey(password, salt, kdf)

        val nonce = randomBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))

        val plaintext = json.encodeToString(content).toByteArray(Charsets.UTF_8)
        val ciphertext = cipher.doFinal(plaintext)

        val envelope = EncryptedBackup(
            kdf = kdf,
            salt = b64(salt),
            nonce = b64(nonce),
            ciphertext = b64(ciphertext)
        )
        return json.encodeToString(envelope)
    }

    fun decrypt(envelopeJson: String, password: String): BackupContent {
        val envelope = json.decodeFromString<EncryptedBackup>(envelopeJson)
        require(envelope.format == "otpbox") { "Unrecognized backup format" }
        val salt = unb64(envelope.salt)
        val nonce = unb64(envelope.nonce)
        val ciphertext = unb64(envelope.ciphertext)
        val key = deriveKey(password, salt, envelope.kdf)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        val plaintext = try {
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw WrongPasswordException()
        }
        return json.decodeFromString(String(plaintext, Charsets.UTF_8))
    }

    private fun deriveKey(password: String, salt: ByteArray, kdf: KdfParams): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, kdf.iterations, kdf.keyBits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also { SecureRandom().nextBytes(it) }

    private fun b64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
    private fun unb64(s: String): ByteArray = Base64.getDecoder().decode(s)

    class WrongPasswordException : Exception("Wrong backup password or corrupted backup")
}
